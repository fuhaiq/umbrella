package org.umbrella.query;

import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;
import org.umbrella.query.session.EngineSession;

import java.util.function.Function;

public record QueryEngineImp(
        EngineWriter writer,
        EngineReader reader,
        EngineSession session,
        BufferAllocator allocator,
        NativeMemoryPool memoryPool,
        FlightClient flightClient,
        ClientIncomingAuthHeaderMiddleware.Factory authFactory
) implements QueryEngine {

    @Override
    public <T> T session(Function<EngineSession, T> func) {
        try(session) {
            session.start();
            return func.apply(session);
        }
    }

    @Override
    public EngineWriter write() {
        return writer;
    }

    @Override
    public EngineCacheHandler cache(String schema, String name) {
        return new EngineCacheHandlerImp(schema, name, allocator, memoryPool, reader, flightClient, authFactory);
    }

    @Override
    public EngineReader reader() {
        return reader;
    }
}
