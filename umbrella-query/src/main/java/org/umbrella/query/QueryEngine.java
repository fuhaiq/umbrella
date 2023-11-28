package org.umbrella.query;

import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.duckdb.DuckDBConnection;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;

@Component
public abstract class QueryEngine {
    public final DSLContext mysql;
    public final DSLContext duckdb;
    @Autowired
    private BufferAllocator allocator;

    public QueryEngine(@Qualifier("mysql") DSLContext mysql, @Qualifier("duckdb") DSLContext duckdb) {
        this.mysql = mysql;
        this.duckdb = duckdb;
    }

    /**
     *
     * @param name - 向 Engine 注册的表名
     * @param reader - Engine 会关闭的,不需要客户端关闭
     * @param func - 执行函数
     * @return - 执行函数的返回值
     */
    public <T> T execute(String name, ArrowReader reader, Function<DSLContext, T> func) {
        return duckdb.connectionResult(conn -> {
            try(conn; reader; var arrowStream = ArrowArrayStream.allocateNew(allocator)) {
                checkState(conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
                Data.exportArrayStream(allocator, reader, arrowStream);
                var duckConn = conn.unwrap(DuckDBConnection.class);
                duckConn.registerArrowStream(name, arrowStream);
                return func.apply(DSL.using(duckConn));
            }
        });
    }
}
