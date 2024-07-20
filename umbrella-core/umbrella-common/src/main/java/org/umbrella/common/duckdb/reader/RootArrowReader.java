package org.umbrella.common.duckdb.reader;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.types.pojo.Schema;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/** An ArrowReader that wraps a list of ArrowRecordBatches. */
public class RootArrowReader extends ArrowReader {
    private final Schema schema;
    private final List<ArrowRecordBatch> batches;
    int nextIndex;

    public RootArrowReader(BufferAllocator allocator, Schema schema, List<ArrowRecordBatch> batches) {
        super(allocator);
        this.schema = schema;
        this.batches = batches;
        this.nextIndex = 0;
    }

    public static ArrowReader fromRoot(BufferAllocator allocator, VectorSchemaRoot root) {
        final ArrowRecordBatch recordBatch = new VectorUnloader(root).getRecordBatch();
        return new RootArrowReader(allocator, root.getSchema(), Collections.singletonList(recordBatch));
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if (nextIndex < batches.size()) {
            new VectorLoader(getVectorSchemaRoot()).load(batches.get(nextIndex++));
            return true;
        }
        return false;
    }

    @Override
    public long bytesRead() {
        return 0;
    }

    @Override
    protected void closeReadSource() throws IOException {
        try {
            AutoCloseables.close(batches);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Schema readSchema() {
        return schema;
    }
}
