package org.umbrella.query.reader;

import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.dataset.source.DatasetFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;

@Slf4j
public class ArrowORCReader extends ArrowReader  {
    private final DatasetFactory datasetFactory;
    private final Dataset dataset;
    private final Scanner scanner;
    private final ArrowReader reader;

    public ArrowORCReader(BufferAllocator allocator, NativeMemoryPool memoryPool, String uri) {
        this(allocator, memoryPool, uri, new ScanOptions.Builder(/*batchSize*/ 32768)
                .columns(Optional.empty())
                .build());
    }

    public ArrowORCReader(BufferAllocator allocator, NativeMemoryPool memoryPool, String uri, ScanOptions options) {
        super(allocator);
        this.datasetFactory = new FileSystemDatasetFactory(
                allocator, memoryPool,
                FileFormat.ORC, uri);
        this.dataset = datasetFactory.finish();
        this.scanner = dataset.newScan(options);
        this.reader = scanner.scanBatches();
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if(!reader.loadNextBatch()) return false; // fast return

        prepareLoadNextBatch();
        try (var root = reader.getVectorSchemaRoot()) {
            final VectorUnloader unloader = new VectorUnloader(root);
            loadRecordBatch(unloader.getRecordBatch());
        }
        return true;
    }

    @Override
    public long bytesRead() {
        return reader.bytesRead();
    }

    @Override
    protected void closeReadSource() throws IOException {
        if(log.isDebugEnabled()) log.debug("释放 ArrowORCReader 资源");
        try {
            reader.close();
            scanner.close();
            dataset.close();
            datasetFactory.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Schema readSchema() {
        return scanner.schema();
    }
}
