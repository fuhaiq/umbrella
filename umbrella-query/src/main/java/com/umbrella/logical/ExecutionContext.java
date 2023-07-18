package com.umbrella.logical;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.RootAllocator;

import java.util.Optional;

public class ExecutionContext {

    public DataFrame parquet(String uri) {
        var allocator = new RootAllocator(Long.MAX_VALUE);
        var factory = new FileSystemDatasetFactory(allocator,
                NativeMemoryPool.getDefault(), FileFormat.PARQUET, uri);
        return new DataFrameImp(new Scan(uri, factory.inspect(), Optional.empty()));
    }

}
