package org.umbrella.query.reader;

import org.apache.arrow.flight.FlightStream;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;

import java.io.IOException;

/**
 * 1. 自动关闭 {@link FlightStream}
 * <br>
 * 2. 注意 {@link ArrowFlightStreamReader#loadNextBatch()} 方法里面不能把 {@link FlightStream#getRoot()} 放入 try-client 里面, 注释说的很清楚:
 * The data in the root may change at any time. Clients should NOT modify the root, but instead unload the data into their own root.
 */
public class ArrowFlightStreamReader extends ArrowReader {

    private final FlightStream stream;

    public ArrowFlightStreamReader(BufferAllocator allocator, FlightStream stream) {
        super(allocator);
        this.stream = stream;
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if(stream.next() && stream.hasRoot()) {
            prepareLoadNextBatch();
            final VectorUnloader unloader = new VectorUnloader(stream.getRoot());
            loadRecordBatch(unloader.getRecordBatch());
            return true;
        } else return false; // fast return
    }

    @Override
    public long bytesRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void closeReadSource() {
        try {
            stream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Schema readSchema() {
        return stream.getSchema();
    }
}
