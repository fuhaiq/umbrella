package com.umbrella.calcite;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.RootAllocator;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

import java.io.Closeable;
import java.io.IOException;

public class ExecutionContext implements Closeable  {

    public RelBuilder parquet(String uri) {
        try (
                var allocator = new RootAllocator();
                var factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, uri)
        ) {
            var schema = factory.inspect();
            var config = Frameworks.newConfigBuilder()
                    .defaultSchema(schema)
                    .build();
            return RelBuilder.create(config);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
