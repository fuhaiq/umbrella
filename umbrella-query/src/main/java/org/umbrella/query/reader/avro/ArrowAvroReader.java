package org.umbrella.query.reader.avro;

import org.apache.arrow.AvroToArrow;
import org.apache.arrow.AvroToArrowConfig;
import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.AvroToArrowVectorIterator;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.avro.io.DecoderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ArrowAvroReader extends ArrowReader  {
    private AvroToArrowVectorIterator iterator;
    private final org.apache.avro.Schema avroSchema;
    private final AvroToArrowConfig config;

    public ArrowAvroReader(BufferAllocator allocator, String file) {
        this(allocator, file, new AvroToArrowConfigBuilder(allocator).build());
    }

    public ArrowAvroReader(BufferAllocator allocator, String file, AvroToArrowConfig config) {
        super(allocator);
        this.config = config;
        try {
            var decoder = new DecoderFactory().binaryDecoder(new FileInputStream(file), null);
            avroSchema = new org.apache.avro.Schema.Parser().parse(new File(file));
            iterator = AvroToArrow.avroToArrowIterator(avroSchema, decoder, this.config);
        } catch (IOException e) {
            if(iterator != null) AutoCloseables.close(e, iterator);
            throw new RuntimeException("创建 Arrow Avro Reader 出错.", e);
        }
    }

    /**
     根据 {@link AvroToArrowVectorIterator} 说明,由客户端释放资源,所以这里使用 try-client
     */
    @Override
    public boolean loadNextBatch() throws IOException {
        if(!iterator.hasNext()) return false; // fast return

        prepareLoadNextBatch();
        try(var root = iterator.next()){
            final VectorUnloader unloader = new VectorUnloader(root);
            loadRecordBatch(unloader.getRecordBatch());
            return true;
        }
    }

    @Override
    public long bytesRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void closeReadSource() {
        iterator.close();
    }

    @Override
    protected Schema readSchema() {
        var columns = avroSchema.getFields().stream().map(f -> AvroToArrowUtils.avroSchemaToField(avroSchema, f.name(), config)).toList();
        return new Schema(columns);
    }
}
