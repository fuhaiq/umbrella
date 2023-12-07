package org.umbrella.query;

import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfig;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.avro.Schema;
import org.duckdb.DuckDBConnection;
import org.duckdb.DuckDBResultSet;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.umbrella.query.reader.ArrowJDBCReader;
import org.umbrella.query.reader.ArrowORCReader;
import org.umbrella.query.reader.avro.ArrowAvroReader;
import org.umbrella.query.session.QuerySession;
import org.umbrella.query.session.SimpleQuerySession;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;

public record QueryEngine(DSLContext duckdb, BufferAllocator allocator) {
    public <T> T orc(String tableName, String uri, Function<DSLContext, T> func) {
        return arrow(tableName, new ArrowORCReader(allocator, uri), func);
    }

    public <T> T orc(String tableName, String uri, String[] columns, Function<DSLContext, T> func) {
        return arrow(tableName, new ArrowORCReader(allocator, uri, new ScanOptions.Builder(/*batchSize*/ 32768)
                .columns(Optional.of(columns))
                .build()), func);
    }

    public <T> T avro(String tableName, String uri, Function<DSLContext, T> func) {
        return arrow(tableName, new ArrowAvroReader(allocator, uri), func);
    }

    public <T> T avro(String tableName, String uri, String[] columns, Function<DSLContext, T> func) {
        Set<String> skipFieldNames = new HashSet<>();
        try {
            var avroSchema = new Schema.Parser().parse(new File(uri));
            var fields = avroSchema.getFields().stream().map(Schema.Field::name).toList();
            for (String col : columns) {
                if (!fields.contains(col)) skipFieldNames.add(col);
            }
        } catch (IOException e) {
            throw new RuntimeException("解析 Avro 文件出错.", e);
        }
        return arrow(tableName, new ArrowAvroReader(allocator, uri, new AvroToArrowConfigBuilder(allocator)
                .setSkipFieldNames(skipFieldNames)
                .build()
        ), func);
    }

    /**
     * 客户端需要手动释放 {@link ResultSet}
     */
    public <T> T jdbc(String tableName, ResultSet rs, Function<DSLContext, T> func) throws SQLException {
        ArrowReader reader;
        if(rs.isWrapperFor(DuckDBResultSet.class)) {
            var drs = rs.unwrap(DuckDBResultSet.class);
            reader = (ArrowReader) drs.arrowExportStream(allocator, JdbcToArrowConfig.DEFAULT_TARGET_BATCH_SIZE);
        } else {
            reader = new ArrowJDBCReader(allocator, rs);
        }
        return arrow(tableName, reader, func);
    }

    public <T> T jdbc(String tableName, ResultQuery<?> rq, Function<DSLContext, T> func) {
        try(var rs = rq.fetchResultSet()) {
            return jdbc(tableName, rs, func);
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public <T> T session(Function<QuerySession, T> func) {
        try(var session = new SimpleQuerySession(this)) {
            session.start();
            return func.apply(session);
        }
    }


    /*
     *
     *
     --------------------------------私有方法--------------------------------
     *
     *
     */

    /**
     * <li>使用了 jOOQ 函数编程, {@link Connection} 会自动回收
     * <li>{@link Data#exportArrayStream} 底层调用 {@link ArrayStreamExporter.ExportedArrayStreamPrivateData} 传输数据,
     * 因为它是一个 {@link Closeable}, 所以会调用 {@link ArrayStreamExporter.ExportedArrayStreamPrivateData#close()} 方法,
     * 而这个方法里面会间接的关闭传入的 {@link ArrowReader}, 所以 try-resource 里面不能把 reader 包进去, 否则会关闭两次
     */
    private <T> T arrow(String tableName, ArrowReader reader, Function<DSLContext, T> func) {
        return duckdb.connectionResult(conn -> {
            try (var arrowStream = ArrowArrayStream.allocateNew(allocator)) {
                checkState(conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
                Data.exportArrayStream(allocator, reader, arrowStream);
                var duckConn = conn.unwrap(DuckDBConnection.class);
                duckConn.registerArrowStream(tableName, arrowStream);
                return func.apply(DSL.using(duckConn));
            }
        });
    }
}
