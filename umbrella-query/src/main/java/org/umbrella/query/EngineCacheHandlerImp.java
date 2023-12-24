package org.umbrella.query;

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
import org.jooq.impl.DSL;
import org.umbrella.query.reader.ArrowArrowReader;
import org.umbrella.query.reader.ArrowFlightStreamReader;
import org.umbrella.query.reader.ArrowORCReader;
import org.umbrella.query.reader.avro.ArrowAvroReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.apache.arrow.util.Preconditions.checkState;

/**
 * 参考: https://duckdb.org/faq.html#how-does-duckdb-handle-concurrency-within-a-single-process
 * <br>
 * 通过 MVCC (Multi-Version Concurrency Control) and optimistic concurrency control(乐观锁) 控制多写并发
 * 应用不用关心并发问题
 */
public record EngineCacheHandlerImp(
        String schema,
        String name,
        BufferAllocator allocator,
        NativeMemoryPool memoryPool,
        EngineReader reader,
        FlightClient flightClient,
        ClientIncomingAuthHeaderMiddleware.Factory authFactory
) implements EngineCacheHandler {
    @Override
    public void dremio(String sql) {
        var auth = authFactory.getCredentialCallOption();
        var info = flightClient.getInfo(FlightDescriptor.command(sql.getBytes(StandardCharsets.UTF_8)), auth);
        try(var stream = flightClient.getStream(info.getEndpoints().get(0).getTicket(), auth)){
            arrow(new ArrowFlightStreamReader(allocator, stream));
        } catch (Exception e) {
            throw new DataAccessException(e.getMessage(), e);
        }
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

    @Override
    public void evict() {
        var sql = String.format("""
        USE %s;
        DROP TABLE IF EXISTS %s;
        """, schema, name);
        reader.duckdb().execute(sql);
    }

    private void arrow(ArrowReader arrowReader) {
        reader.duckdb().connection(conn -> {
            checkState(conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            try (arrowReader;var arrowStream = ArrowArrayStream.allocateNew(allocator)) {
                Data.exportArrayStream(allocator, arrowReader, arrowStream);
                var duckConn = conn.unwrap(DuckDBConnection.class);
                duckConn.registerArrowStream("CACHE_TMP", arrowStream);
                var sql = String.format("""
                CREATE SCHEMA IF NOT EXISTS %s;
                USE %s;
                DROP TABLE IF EXISTS %s;
                CREATE TABLE %s AS SELECT * FROM CACHE_TMP;
                """, schema, schema, name, name);
                DSL.using(conn).execute(sql);
            }
        });
    }
}
