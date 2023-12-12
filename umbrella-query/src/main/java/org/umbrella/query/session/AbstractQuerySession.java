package org.umbrella.query.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfig;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.avro.Schema;
import org.duckdb.DuckDBConnection;
import org.duckdb.DuckDBResultSet;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.umbrella.query.QueryEngine;
import org.umbrella.query.reader.ArrowArrowReader;
import org.umbrella.query.reader.ArrowJDBCReader;
import org.umbrella.query.reader.ArrowORCReader;
import org.umbrella.query.reader.avro.ArrowAvroReader;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.apache.arrow.util.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractQuerySession implements QuerySession{

    protected final QueryEngine engine;

    abstract QuerySessionElement element();

    @Override
    public void jdbc(String tableName, ResultSet rs) throws SQLException {
        ArrowReader reader;
        if (rs.isWrapperFor(DuckDBResultSet.class)) {
            if(log.isDebugEnabled()) log.debug("Zero-Copy 导出 JDBC 数据");
            var drs = rs.unwrap(DuckDBResultSet.class);
            reader = (ArrowReader) drs.arrowExportStream(engine.allocator(), JdbcToArrowConfig.DEFAULT_TARGET_BATCH_SIZE);
        } else {
            reader = new ArrowJDBCReader(engine.allocator(), rs);
        }
        arrow(tableName, reader);
    }

    /**
     * 获取 {@link ResultSet}, 并注册到 Arrow, 这里只是注册,并没有消费,所以不能使用 try-resource
     */
    @Override
    public void jdbc(String tableName, ResultQuery<?> rq) {
        try {
            jdbc(tableName, rq.fetchResultSet());
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    @Override
    public void orc(String tableName, String uri) {
        arrow(tableName, new ArrowORCReader(engine.allocator(), engine.memoryPool(), uri));
    }

    @Override
    public void orc(String tableName, String uri, String[] columns) {
        arrow(tableName, new ArrowORCReader(engine.allocator(), engine.memoryPool(), uri, new ScanOptions.Builder(/*batchSize*/ 32768)
                .columns(Optional.of(columns))
                .build()));
    }

    @Override
    public void arrow(String tableName, String uri) {
        arrow(tableName, new ArrowArrowReader(engine.allocator(), engine.memoryPool(), uri));
    }

    @Override
    public void arrow(String tableName, String uri, String[] columns) {
        arrow(tableName, new ArrowArrowReader(engine.allocator(), engine.memoryPool(), uri, new ScanOptions.Builder(/*batchSize*/ 32768)
                .columns(Optional.of(columns))
                .build()));
    }

    @Override
    public void avro(String tableName, String uri) {
        arrow(tableName, new ArrowAvroReader(engine.allocator(), uri));
    }

    @Override
    public void avro(String tableName, String uri, String[] columns) {
        Set<String> skipFieldNames = new HashSet<>();
        try {
            var avroSchema = new Schema.Parser().parse(new File(uri));
            var fields = avroSchema.getFields().stream().map(Schema.Field::name).toList();
            for (String col : columns) {
                if (!fields.contains(col)) skipFieldNames.add(col);
            }
        } catch (IOException e) {
            throw new org.jooq.exception.IOException("解析 Avro 文件出错.", e);
        }
        arrow(tableName, new ArrowAvroReader(engine.allocator(), uri, new AvroToArrowConfigBuilder(engine.allocator())
                .setSkipFieldNames(skipFieldNames)
                .build()
        ));
    }

    @Override
    public DSLContext dsl() {
        checkState(element() != null, "获取 Arrow 会话失败,会话未开启.");
        return DSL.using(element().conn());
    }

    @Override
    public void close() {
        checkState(element() != null, "关闭 Arrow 会话失败,会话已经关闭.");
        element().map().values().forEach(ArrowArrayStream::close);
        engine.duckdb().configuration().connectionProvider().release(element().conn());
        if(log.isDebugEnabled()) log.debug("关闭 Arrow 会话");
    }

    /*
     *
     *
     --------------------------------私有方法--------------------------------
     *
     *
     */

    /**
     <li>{@link Data#exportArrayStream} 底层调用 {@link ArrayStreamExporter.ExportedArrayStreamPrivateData} 传输数据,
     * 因为它是一个 {@link Closeable}, 所以会调用 {@link ArrayStreamExporter.ExportedArrayStreamPrivateData#close()} 方法,
     * 而这个方法里面会间接的关闭传入的 {@link ArrowReader}, 所以 {@link AbstractQuerySession#close()} 方法不需要关闭 {@link ArrowReader}
     */
    private void arrow(String tableName, ArrowReader reader) {
        checkState(element() != null, "获取 Arrow 会话失败,会话未开启.");
        try {
            var stream = ArrowArrayStream.allocateNew(engine.allocator());
            checkState(element().conn().isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            Data.exportArrayStream(engine.allocator(), reader, stream);
            var duckConn = element().conn().unwrap(DuckDBConnection.class);
            duckConn.registerArrowStream(tableName, stream);
            element().map().put(tableName, stream);
            if(log.isDebugEnabled()) log.debug("导出数据到 Arrow 会话完成.");
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }
}
