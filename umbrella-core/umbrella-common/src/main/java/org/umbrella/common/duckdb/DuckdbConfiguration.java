package org.umbrella.common.duckdb;

import lombok.RequiredArgsConstructor;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DuckdbConfiguration {

    @Bean
    public DuckdbSession dbsession(BufferAllocator allocator, SqlSessionFactory sqlSessionFactory) {
        return new DuckdbSessionImpl(allocator, sqlSessionFactory);
    }
}