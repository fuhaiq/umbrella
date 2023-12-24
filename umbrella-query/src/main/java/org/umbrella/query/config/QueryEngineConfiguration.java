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
    public EngineSession engineSession(EngineReader reader,
                                       BufferAllocator allocator,
                                       NativeMemoryPool memoryPool,
                                       FlightClient flightClient,
                                       ClientIncomingAuthHeaderMiddleware.Factory authFactory) {
        return new ThreadLocalEngineSession(reader, allocator, memoryPool, flightClient, authFactory);
    }
    @Bean
    public QueryEngine queryEngine(EngineWriter writer,
                                     EngineReader query,
                                     EngineSession session,
                                     BufferAllocator allocator,
                                     NativeMemoryPool memoryPool,
                                     FlightClient flightClient,
                                     ClientIncomingAuthHeaderMiddleware.Factory authFactory
    ) {
        return new QueryEngineImp(writer, query, session, allocator, memoryPool, flightClient, authFactory);
    }


}
