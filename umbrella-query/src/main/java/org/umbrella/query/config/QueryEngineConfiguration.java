package org.umbrella.query.config;

import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.umbrella.query.*;
import org.umbrella.query.session.EngineSession;
import org.umbrella.query.session.ThreadLocalEngineSession;

@Configuration
public class QueryEngineConfiguration {

    @Bean
    public EngineWriter engineWriter(@Qualifier("mysql") DSLContext mysql) {
        return new EngineWriterImp(mysql);
    }

    @Bean
    public EngineReader engineReader(@Qualifier("duckdb") DSLContext duckdb, @Qualifier("dremio") DSLContext dremio) {
        return new EngineReaderImp(duckdb, dremio);
    }

    @Bean
    public EngineClient engineClient(EngineReader reader,
                                       EngineWriter writer,
                                       BufferAllocator allocator,
                                       NativeMemoryPool memoryPool,
                                       FlightClient flightClient,
                                       ClientIncomingAuthHeaderMiddleware.Factory authFactory) {
        return new EngineClient(allocator, reader, writer, memoryPool, flightClient, authFactory);
    }

    @Bean
    public EngineSession engineSession(EngineClient engineClient) {
        return new ThreadLocalEngineSession(engineClient);
    }
    @Bean
    public QueryEngine queryEngine(EngineClient engineClient,
                                   EngineSession engineSession
    ) {
        return new QueryEngineImp(engineClient, engineSession);
    }


}
