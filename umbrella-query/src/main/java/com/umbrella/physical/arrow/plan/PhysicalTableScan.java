package com.umbrella.physical.arrow.plan;

import com.google.common.collect.Iterables;
import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.util.TransferPair;
import org.apache.arrow.vector.util.VectorBatchAppender;
import org.apache.arrow.vector.util.VectorSchemaRootAppender;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

public record PhysicalTableScan(String uri, FileFormat format, Optional<String[]> projection) implements PhysicalPlan{

    @Override
    public List<PhysicalPlan> getInputs() {
        return List.of();
    }

    @Override
    public VectorSchemaRoot execute() {
        VectorSchemaRoot result = null;
        var options = new ScanOptions(/*batchSize*/ 32768, projection);
        try (
                var datasetFactory = new FileSystemDatasetFactory(ExecutionContext.instance().allocator(),
                        NativeMemoryPool.getDefault(), format, uri);
                var dataset = datasetFactory.finish();
                var scanner = dataset.newScan(options);
                var reader = scanner.scanBatches()
        ) {
            while (reader.loadNextBatch()) {
                try (var root = reader.getVectorSchemaRoot()) {
                    if(result == null) {
                        result = VectorSchemaRoot.create(root.getSchema(), ExecutionContext.instance().allocator());
                    }

                    var fields = root.getFieldVectors();
                    for(int i = 0; i < fields.size(); i++) {
                        fields.get(i).makeTransferPair(result.getVector(i)).transfer();
                    }
                    result.setRowCount(result.getRowCount() + root.getRowCount());
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        String project = projection.map(strings -> "[" + StringUtils.join(strings, ",") + "]").orElse("NONE");
        return "PhysicalTableScan{" +
                "uri='" + uri + '\'' +
                ", projection=" + project +
                '}';
    }
}
