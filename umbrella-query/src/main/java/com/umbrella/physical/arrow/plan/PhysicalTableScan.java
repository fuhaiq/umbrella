package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.util.VectorSchemaRootAppender;
import org.apache.calcite.rex.RexNode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

//TODO implements FILTER_SCAN
public record PhysicalTableScan(String uri, FileFormat format, Optional<String[]> projection, Optional<RexNode[]> filters) implements PhysicalPlan{

    @Override
    public List<PhysicalPlan> getInputs() {
        return List.of();
    }

    @Override
    public VectorBatch execute() {
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
                        result.allocateNew();
                    }
                    VectorSchemaRootAppender.append(result, root);
                }
            }
            return VectorBatch.of(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        String project = projection.map(strings -> "[" + StringUtils.join(strings, ",") + "]").orElse("NONE");
        String filter = filters.map(strings -> "[" + StringUtils.join(strings, ",") + "]").orElse("NONE");
        return "PhysicalTableScan{" +
                "uri='" + uri + '\'' +
                ", projection=" + project +
                ", filter=" + filter +
                '}';
    }
}
