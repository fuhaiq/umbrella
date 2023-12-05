package org.umbrella.query.session;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.vector.ipc.ArrowReader;
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

import java.io.IOException;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
public class FastThreadLocalQuerySession implements QuerySession {
    private final FastThreadLocal<QuerySessionElement> threadLocal = new FastThreadLocal<>();
    private final QueryEngine engine;
    @Override
    public void start() {
        checkState(!threadLocal.isSet(), "开启 Arrow 会话失败,会话已经开启.");
        var conn = engine.duckdb().configuration().connectionProvider().acquire();
        threadLocal.set(new QuerySessionElement(conn));
        if(log.isDebugEnabled()) log.debug("开启 Arrow 会话");
    }

    @Override
    public void jdbc(String tableName, ResultQuery<?> rq) {
        arrow(tableName, new ArrowResultQueryReader(engine.allocator(), rq));
    }

    @Override
    public void orc(String tableName, String uri) {
        arrow(tableName, new ArrowORCReader(engine.allocator(), uri));
    }

    @Override
    public void avro(String tableName, String uri) {
        arrow(tableName, new ArrowAvroReader(engine.allocator(), uri));
    }

    @Override
    public DSLContext dsl() {
        checkState(threadLocal.isSet(), "获取 Arrow 会话失败,会话未开启.");
        return DSL.using(threadLocal.get().conn);
    }

    @Override
    public void close() {
        checkState(threadLocal.isSet(), "关闭 Arrow 会话失败,会话已经关闭.");
        var ele = threadLocal.get();
        try {
            for(String key : ele.map.keySet()) {
                var pair = ele.map.get(key);
                pair.v1.close();
                pair.v2.close();
            }
        } catch (IOException e) {
            throw new org.jooq.exception.IOException(e.getMessage(), e);
        } finally {
            engine.duckdb().configuration().connectionProvider().release(ele.conn);
            threadLocal.remove();
            if(log.isDebugEnabled()) log.debug("关闭 Arrow 会话");
        }
    }

    private void arrow(String tableName, ArrowReader reader) {
        checkState(threadLocal.isSet(), "获取 Arrow 会话失败,会话未开启.");
        try {
            var stream = ArrowArrayStream.allocateNew(engine.allocator());
            var ele = threadLocal.get();
            checkState(ele.conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            Data.exportArrayStream(engine.allocator(), reader, stream);
            var duckConn = ele.conn.unwrap(DuckDBConnection.class);
            duckConn.registerArrowStream(tableName, stream);

            ele.map.put(tableName, new Tuple2<>(stream, reader));
            if(log.isDebugEnabled()) log.debug("导出数据到 Arrow 会话完成.");
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }
}
