package org.umbrella.common.duckdb;

import org.apache.arrow.memory.AllocationListener;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.memory.rounding.DefaultRoundingPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new RootAllocator(
                allocationListener, maxAllocation, DefaultRoundingPolicy.DEFAULT_ROUNDING_POLICY);
    }
}
