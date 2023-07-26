package com.umbrella.execution;

import com.umbrella.logical.DataFrame;
import com.umbrella.logical.DataFrameImp;
import com.umbrella.logical.Scan;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;

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

    public DataFrame parquet(String uri) {
        return parquet(uri, Optional.empty());
    }

    public DataFrame parquet(String uri, Optional<String[]> projection) {
        try (
                var factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, uri)
        ) {
            return new DataFrameImp(new Scan(uri, factory.inspect(), projection));
        }
    }

    public void stop() {
        allocator.close();
    }
}
