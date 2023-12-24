package org.umbrella.query;

import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.avro.Schema;
import org.umbrella.query.reader.ArrowArrowReader;
import org.umbrella.query.reader.ArrowORCReader;
import org.umbrella.query.reader.avro.ArrowAvroReader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class EngineHandlerImp implements EngineHandler {

    protected final BufferAllocator allocator;
    protected final NativeMemoryPool memoryPool;
    protected final EngineReader reader;
    protected final FlightClient flightClient;
    protected final ClientIncomingAuthHeaderMiddleware.Factory authFactory;

    public EngineHandlerImp(BufferAllocator allocator, NativeMemoryPool memoryPool, EngineReader reader, FlightClient flightClient, ClientIncomingAuthHeaderMiddleware.Factory authFactory) {
        this.allocator = allocator;
        this.memoryPool = memoryPool;
        this.reader = reader;
        this.flightClient = flightClient;
        this.authFactory = authFactory;
    }

    @Override
    public void orc(String file) {
        arrow(new ArrowORCReader(allocator, memoryPool, file));
    }

    @Override
    public void orc(String file, String[] columns) {
        arrow(new ArrowORCReader(allocator, memoryPool, file,
                new ScanOptions.Builder(/*batchSize*/ 32768).columns(Optional.of(columns))
                        .build()
        ));
    }

    @Override
    public void arrow(String file) {
        arrow(new ArrowArrowReader(allocator, memoryPool, file));
    }

    @Override
    public void arrow(String file, String[] columns) {
        arrow(new ArrowArrowReader(allocator, memoryPool, file,
                new ScanOptions.Builder(/*batchSize*/ 32768).columns(Optional.of(columns))
                        .build()
        ));
    }

    @Override
    public void avro(String file) {
        arrow(new ArrowAvroReader(allocator, file));
    }

    @Override
    public void avro(String file, String[] columns) {
        Set<String> skipFieldNames = new HashSet<>();
        try {
            var avroSchema = new Schema.Parser().parse(new File(file));
            var fields = avroSchema.getFields().stream().map(Schema.Field::name).toList();
            for (String col : columns) {
                if (!fields.contains(col)) skipFieldNames.add(col);
            }
        } catch (IOException e) {
            throw new org.jooq.exception.IOException("解析 Avro 文件出错.", e);
        }
        arrow(new ArrowAvroReader(allocator, file, new AvroToArrowConfigBuilder(allocator)
                .setSkipFieldNames(skipFieldNames)
                .build()
        ));
    }

    abstract void arrow(ArrowReader arrowReader);
}
