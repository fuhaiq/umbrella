package org.umbrella.query.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.avro.Schema;
import org.duckdb.DuckDBConnection;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.lambda.tuple.Tuple2;
import org.umbrella.query.QueryEngine;
import org.umbrella.query.reader.ArrowORCReader;
import org.umbrella.query.reader.ArrowResultQueryReader;
import org.umbrella.query.reader.avro.ArrowAvroReader;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractQuerySession implements QuerySession{

    protected final QueryEngine engine;

    protected abstract QuerySessionElement element();

    @Override
    public void jdbc(String tableName, ResultQuery<?> rq) {
        arrow(tableName, new ArrowResultQueryReader(engine.allocator(), rq));
    }

    @Override
    public void orc(String tableName, String uri) {
        arrow(tableName, new ArrowORCReader(engine.allocator(), uri));
    }

    @Override
    public void orc(String tableName, String uri, String[] columns) {
        arrow(tableName, new ArrowORCReader(engine.allocator(), uri, new ScanOptions.Builder(/*batchSize*/ 32768)
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
        try {
            for(String key : element().map().keySet()) {
                var pair = element().map().get(key);
                pair.v1.close();
                pair.v2.close();
            }
        } catch (IOException e) {
            throw new org.jooq.exception.IOException(e.getMessage(), e);
        } finally {
            engine.duckdb().configuration().connectionProvider().release(element().conn());
            if(log.isDebugEnabled()) log.debug("关闭 Arrow 会话");
        }
    }

    private void arrow(String tableName, ArrowReader reader) {
        checkState(element() != null, "获取 Arrow 会话失败,会话未开启.");
        try {
            var stream = ArrowArrayStream.allocateNew(engine.allocator());
            checkState(element().conn().isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            Data.exportArrayStream(engine.allocator(), reader, stream);
            var duckConn = element().conn().unwrap(DuckDBConnection.class);
            duckConn.registerArrowStream(tableName, stream);

            element().map().put(tableName, new Tuple2<>(stream, reader));
            if(log.isDebugEnabled()) log.debug("导出数据到 Arrow 会话完成.");
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }
}
