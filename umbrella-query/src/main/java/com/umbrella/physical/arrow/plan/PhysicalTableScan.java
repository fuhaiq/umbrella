package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.util.VectorSchemaRootAppender;

import java.util.List;
import java.util.Optional;

public record PhysicalTableScan(String uri, Optional<String[]> projection) implements PhysicalPlan{

    @Override
    public List<PhysicalPlan> getInputs() {
        return List.of();
    }

    @Override
    public VectorSchemaRoot execute() {
        VectorSchemaRoot result = null;
        ScanOptions options = new ScanOptions(/*batchSize*/ 32768, projection);
        try (
                var datasetFactory = new FileSystemDatasetFactory(ExecutionContext.instance().allocator(),
                        NativeMemoryPool.getDefault(), FileFormat.PARQUET, uri);
                Dataset dataset = datasetFactory.finish();
                Scanner scanner = dataset.newScan(options);
                ArrowReader reader = scanner.scanBatches()
        ) {
            while (reader.loadNextBatch()) {
                try (VectorSchemaRoot root = reader.getVectorSchemaRoot()) {
                    if(result == null) {
                        result = VectorSchemaRoot.create(root.getSchema(), ExecutionContext.instance().allocator());
                    }
                    VectorSchemaRootAppender.append(result, root);
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "PhysicalTableScan{" +
                "uri='" + uri + '\'' +
                ", projection=" + projection +
                '}';
    }
}
