package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.umbrella.query.EngineReader;

import java.sql.Connection;

import static cdjd.org.apache.arrow.util.Preconditions.checkState;

@Slf4j
public abstract class AbstractEngineSession implements EngineSession {

    protected final EngineReader reader;
    protected final BufferAllocator allocator;
    protected final NativeMemoryPool memoryPool;
    protected final FlightClient flightClient;
    protected final ClientIncomingAuthHeaderMiddleware.Factory authFactory;

    public AbstractEngineSession(EngineReader reader, BufferAllocator allocator, NativeMemoryPool memoryPool, FlightClient flightClient, ClientIncomingAuthHeaderMiddleware.Factory authFactory) {
        this.reader = reader;
        this.allocator = allocator;
        this.memoryPool = memoryPool;
        this.flightClient = flightClient;
        this.authFactory = authFactory;
    }

    abstract EngineSessionResource resource();

    @Override
    public DSLContext dsl() {
        final var element = resource();
        checkState(element != null, "获取 Arrow 会话失败,会话未开启.");
        return DSL.using(element.conn());
    }

    @Override
    public EngineSessionHandler define(String name) {
        checkState(resource() != null, "获取 Arrow 会话失败,会话未开启.");
        return new EngineSessionHandlerImp(name, resource(), allocator, memoryPool, reader, flightClient, authFactory);
    }

    @Override
    public void start() {
        checkState(resource() == null, "开启 Arrow 会话失败,会话已经开启.");
        var conn = reader.duckdb().configuration().connectionProvider().acquire();
        startSession(conn);
        if(log.isDebugEnabled()) log.debug("开启 Arrow 会话");
    }

    abstract void startSession(Connection conn);

    @Override
    public void close() {
        checkState(resource() != null, "关闭 Arrow 会话失败,会话已经关闭.");
        try(var element = resource()) {
            closeSession();
            reader.duckdb().configuration().connectionProvider().release(element.conn());
        }
        if(log.isDebugEnabled()) log.debug("关闭 Arrow 会话");
    }

    abstract void closeSession();
}
