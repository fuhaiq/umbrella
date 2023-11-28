package org.umbrella.query.config;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArrowConfiguration {

    @Bean(destroyMethod = "close")
    public BufferAllocator allocator() {
        return new RootAllocator();
    }

}
