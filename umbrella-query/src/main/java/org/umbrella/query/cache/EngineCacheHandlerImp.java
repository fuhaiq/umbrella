package org.umbrella.query.cache;

import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.duckdb.DuckDBConnection;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.umbrella.query.EngineClient;
import org.umbrella.query.EngineHandlerImp;
import org.umbrella.query.reader.ArrowFlightStreamReader;

import java.nio.charset.StandardCharsets;

import static org.apache.arrow.util.Preconditions.checkState;

/**
 * 参考: https://duckdb.org/faq.html#how-does-duckdb-handle-concurrency-within-a-single-process
 * <br>
 * 通过 MVCC (Multi-Version Concurrency Control) and optimistic concurrency control(乐观锁) 控制多写并发
 * 应用不用关心并发问题
 */
public class EngineCacheHandlerImp extends EngineHandlerImp implements EngineCacheHandler {
    private final String schema;
    private final String name;
    public EngineCacheHandlerImp(String schema, String name, EngineClient client) {
        super(client);
        this.schema = schema;
        this.name = name;
    }

    @Override
    public void dremio(String sql) {
        var flightClient = client.flightClient();
        var auth = client.authFactory().getCredentialCallOption();
        var info = flightClient.getInfo(FlightDescriptor.command(sql.getBytes(StandardCharsets.UTF_8)), auth);
        try(var stream = flightClient.getStream(info.getEndpoints().get(0).getTicket(), auth)){
            arrow(new ArrowFlightStreamReader(client.allocator(), stream));
        } catch (Exception e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    @Override
    public void evict() {
        var sql = String.format("""
        USE %s;
        DROP TABLE IF EXISTS %s;
        """, schema, name);
        client.reader().duckdb().execute(sql);
    }

    @Override
    public void arrow(ArrowReader reader) {
        client.reader().duckdb().connection(conn -> {
            checkState(conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            try (reader;var arrowStream = ArrowArrayStream.allocateNew(client.allocator())) {
                Data.exportArrayStream(client.allocator(), reader, arrowStream);
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
