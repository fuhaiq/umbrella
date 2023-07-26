package com.umbrella.calcite.adapter.parquet;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.dataset.source.DatasetFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.calcite.linq4j.Enumerator;
import static com.google.common.base.Preconditions.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class ParquetEnumerator<T> implements Enumerator<T> {

    private final AtomicBoolean cancelFlag;
    private T current;
    private final BufferAllocator allocator;
    private final DatasetFactory datasetFactory;
    private final Dataset dataset;
    private final Scanner scanner;
    private final ArrowReader reader;

    public ParquetEnumerator(AtomicBoolean cancelFlag, String uri) {
        this.cancelFlag = cancelFlag;
        ScanOptions options = new ScanOptions(/*batchSize*/ 1);
        allocator = new RootAllocator();
        datasetFactory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, uri);
        dataset = datasetFactory.finish();
        scanner = dataset.newScan(options);
        reader = scanner.scanBatches();
    }

    @Override
    public T current() {
        return this.current;
    }

    @Override
    public boolean moveNext() {
        try {
            while(!cancelFlag.get() && reader.loadNextBatch()) {
                try (var root = reader.getVectorSchemaRoot()) {
                    checkState(root.getRowCount() == 1, "返回多行记录");

                }
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            reader.close();
            scanner.close();
            dataset.close();
            datasetFactory.close();
            allocator.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
