package com.umbrella.physical.arrow;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;

import java.util.Objects;

public final class ExecutionContext {
    private ExecutionContext(){
        allocator = new RootAllocator();
    }

    private final BufferAllocator allocator;

    private volatile static ExecutionContext instance;

    public static ExecutionContext instance() {
        if(Objects.isNull(instance)) {
            synchronized (ExecutionContext.class) { // can't use instance as lock for it is null here
                if(Objects.isNull(instance)) {
                    instance = new ExecutionContext();
                }
            }
        }
        return instance;
    }

    public BufferAllocator allocator() {
        return allocator;
    }

    public void stop() {
        allocator.close();
    }
}
