package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;
import org.umbrella.query.EngineReader;

import java.sql.Connection;
import java.util.ArrayList;

@Slf4j
public class ThreadLocalEngineSession extends AbstractEngineSession {
    private final ThreadLocal<EngineSessionResource> threadLocal = new ThreadLocal<>();

    public ThreadLocalEngineSession(EngineReader reader, BufferAllocator allocator, NativeMemoryPool memoryPool, FlightClient flightClient, ClientIncomingAuthHeaderMiddleware.Factory authFactory) {
        super(reader, allocator, memoryPool, flightClient, authFactory);
    }

    @Override
    EngineSessionResource resource() {
        return threadLocal.get();
    }

    @Override
    void closeSession() {
        threadLocal.remove();
    }

    @Override
    void startSession(Connection conn) {
        threadLocal.set(new EngineSessionResource(conn, new ArrayList<>()));
    }
}
