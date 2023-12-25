package org.umbrella.query;

import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;

public record EngineClient(
        BufferAllocator allocator,
        EngineReader reader,
        EngineWriter writer,
        NativeMemoryPool memoryPool,
        FlightClient flightClient,
        ClientIncomingAuthHeaderMiddleware.Factory authFactory
) {
}
