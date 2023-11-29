package org.umbrella.query.config;

import com.google.common.collect.ImmutableMap;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.umbrella.query.QueryEngine;

@Configuration
public class ArrowConfiguration {

    @Bean(destroyMethod = "close")
    public BufferAllocator allocator() {
        return new RootAllocator();
    }

    @Bean
    public QueryEngine engine(@Qualifier("mysql") DSLContext mysql, @Qualifier("duckdb") DSLContext duckdb,
                              BufferAllocator allocator) {
        return new QueryEngine(ImmutableMap.of(
                "mysql", mysql
        ), duckdb, allocator);
    }

}
