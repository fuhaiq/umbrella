package com.umbrella.execution;

import com.umbrella.logical.DataFrame;
import com.umbrella.logical.DataFrameImp;
import com.umbrella.logical.Scan;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.RootAllocator;

import java.util.Optional;

public class ExecutionContext {

    public DataFrame parquet(String uri) {
        return parquet(uri, Optional.empty());
    }

    public DataFrame parquet(String uri, Optional<String[]> projection) {
        try (
                var allocator = new RootAllocator();
                var factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, uri)
        ) {
            return new DataFrameImp(new Scan(uri, factory.inspect(), projection));
        }
    }

}
