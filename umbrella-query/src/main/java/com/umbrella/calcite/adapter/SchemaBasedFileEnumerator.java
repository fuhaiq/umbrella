package com.umbrella.calcite.adapter;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.dataset.source.DatasetFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rex.RexCall;

import static com.google.common.base.Preconditions.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SchemaBasedFileEnumerator<T> implements Enumerator<T> {

    private final AtomicBoolean cancelFlag;
    private int current_count;
    private final BufferAllocator allocator;
    private final DatasetFactory datasetFactory;
    private final Dataset dataset;
    private final Scanner scanner;
    private final ArrowReader reader;
    private VectorSchemaRoot root;

    private final List<RexCall> filters;

    public SchemaBasedFileEnumerator(AtomicBoolean cancelFlag, String uri, FileFormat format, ScanOptions options, List<RexCall> filters) {
        this.cancelFlag = cancelFlag;
        this.filters = filters;
        allocator = new RootAllocator();
        datasetFactory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), format, uri);
        dataset = datasetFactory.finish();
        scanner = dataset.newScan(options);
        reader = scanner.scanBatches();

    }

    @Override
    public T current() {
        var columns = root.getFieldVectors().size();
        var row = new Object[columns];
        for (var i = 0; i < columns; i++) {
            row[i] = root.getVector(i).getObject(root.getRowCount() - current_count);
        }
        current_count--;
        return (T) row;
    }

    @Override
    public boolean moveNext() {
        if(cancelFlag.get()) return false;
        try {
            if(root == null && reader.loadNextBatch()) {
                checkState(current_count == 0, "第一次读取,但当前批次仍存在数据");
                root = reader.getVectorSchemaRoot();
                return (current_count = root.getRowCount()) != 0;
            }
            if(current_count != 0) {
                checkNotNull(root, "继续读取当前批次,但数据集为空");
                return true;
            }
            if(reader.loadNextBatch()) {
                checkState(current_count == 0, "读取下一个批次,但上一个批次数据没有消耗完");
                root.close(); // 释放上一次批次数据
                root = reader.getVectorSchemaRoot();
                return (current_count = root.getRowCount()) != 0;
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
            root.close();
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
