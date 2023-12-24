package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.AvroToArrowConfigBuilder;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.flight.FlightClient;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.flight.auth2.ClientIncomingAuthHeaderMiddleware;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.avro.Schema;
import org.duckdb.DuckDBConnection;
import org.jooq.exception.DataAccessException;
import org.umbrella.query.EngineReader;
import org.umbrella.query.reader.ArrowArrowReader;
import org.umbrella.query.reader.ArrowFlightStreamReader;
import org.umbrella.query.reader.ArrowORCReader;
import org.umbrella.query.reader.avro.ArrowAvroReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static cdjd.org.apache.arrow.util.Preconditions.checkState;

@Slf4j
public record EngineSessionHandlerImp(
        String name,
        EngineSessionResource resource,
        BufferAllocator allocator,
        NativeMemoryPool memoryPool,
        EngineReader reader,
        FlightClient flightClient,
        ClientIncomingAuthHeaderMiddleware.Factory authFactory
) implements EngineSessionHandler {
    @Override
    public void dremio(String sql) {
        var auth = authFactory.getCredentialCallOption();
        var info = flightClient.getInfo(FlightDescriptor.command(sql.getBytes(StandardCharsets.UTF_8)), auth);
        var stream = flightClient.getStream(info.getEndpoints().get(0).getTicket(), auth);
        resource.addResource(stream); // 在 arrow 方法里面统一 addResource(reader)
        arrow(new ArrowFlightStreamReader(allocator, stream));
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

    private void arrow(ArrowReader arrowReader) {
        try {
            var stream = ArrowArrayStream.allocateNew(allocator);
            resource.addResource(stream, arrowReader);
            checkState(resource.conn().isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            Data.exportArrayStream(allocator, arrowReader, stream);
            var duckConn = resource.conn().unwrap(DuckDBConnection.class);
            duckConn.registerArrowStream(name, stream);
            if(log.isDebugEnabled()) log.debug("导出数据到 Arrow 会话完成.");
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }
}
