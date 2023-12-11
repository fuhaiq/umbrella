package org.umbrella.query.config;

import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.AllocationListener;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.memory.rounding.DefaultRoundingPolicy;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.umbrella.query.QueryEngine;

@Configuration
public class ArrowConfiguration {

    /**
     * max allocation size in bytes, default to Long.MAX_VALUE
     */
    @Value("${spring.arrow.max-allocation:#{T(java.lang.Long).MAX_VALUE}}")
    private long maxAllocation;

    @Bean
    public AllocationListener allocationListener() {
        return new LogAllocationListener();
    }

    @Bean(destroyMethod = "close")
    public BufferAllocator allocator(AllocationListener allocationListener) {
        return new RootAllocator(allocationListener, maxAllocation, DefaultRoundingPolicy.DEFAULT_ROUNDING_POLICY);
    }

    @Bean(destroyMethod = "close")
    public NativeMemoryPool memoryPool() {
//        return NativeMemoryPool.createListenable(DirectReservationListener.instance());
        return NativeMemoryPool.getDefault();
    }

    @Bean
    public QueryEngine engine(@Qualifier("duckdb") DSLContext duckdb,
                              BufferAllocator allocator, NativeMemoryPool memoryPool) {
        return new QueryEngine(duckdb, allocator, memoryPool);
    }

}
