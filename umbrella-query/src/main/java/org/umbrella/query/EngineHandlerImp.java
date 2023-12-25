package org.umbrella.query;

import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.dataset.scanner.ScanOptions;
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

    protected final EngineClient client;

    public EngineHandlerImp(EngineClient client) {
        this.client = client;
    }

    @Override
    public void orc(String file) {
        arrow(new ArrowORCReader(client.allocator(), client.memoryPool(), file));
    }

    @Override
    public void orc(String file, String[] columns) {
        arrow(new ArrowORCReader(client.allocator(), client.memoryPool(), file,
                new ScanOptions.Builder(/*batchSize*/ 32768).columns(Optional.of(columns))
                        .build()
        ));
    }

    @Override
    public void arrow(String file) {
        arrow(new ArrowArrowReader(client.allocator(), client.memoryPool(), file));
    }

    @Override
    public void arrow(String file, String[] columns) {
        arrow(new ArrowArrowReader(client.allocator(), client.memoryPool(), file,
                new ScanOptions.Builder(/*batchSize*/ 32768).columns(Optional.of(columns))
                        .build()
        ));
    }

    @Override
    public void avro(String file) {
        arrow(new ArrowAvroReader(client.allocator(), file));
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
        arrow(new ArrowAvroReader(client.allocator(), file, new AvroToArrowConfigBuilder(client.allocator())
                .setSkipFieldNames(skipFieldNames)
                .build()
        ));
    }
}
