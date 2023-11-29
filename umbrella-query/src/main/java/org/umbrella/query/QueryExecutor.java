package org.umbrella.query;

import lombok.RequiredArgsConstructor;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.duckdb.DuckDBConnection;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.impl.DSL;
import org.umbrella.query.reader.ArrowJDBCReader;
import org.umbrella.query.reader.ArrowORCReader;

import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkState;

@RequiredArgsConstructor
public class QueryExecutor {
    private final String name;
    private final QueryEngine engine;

    public <T> T orc(String name, Function<DSLContext, T> func) {
        return arrow(new ArrowORCReader(engine.allocator, name), func);
    }

    public <T> T orc(String name, Function<DSLContext, T> func, String... columns) {
        return arrow(new ArrowORCReader(engine.allocator, name,
                new ScanOptions.Builder(/*batchSize*/ 32768)
                        .columns(Optional.of(columns))
                        .build()), func);
    }

    public <T> T jdbc(ResultQuery<?> rq, Function<DSLContext, T> func) {
        try(var rs = rq.fetchResultSet()) {
            return arrow(new ArrowJDBCReader(engine.allocator, rs), func);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T arrow(ArrowReader reader, Function<DSLContext, T> func) {
        final var duckdb = engine.duckdb;
        final var allocator = engine.allocator;
        return duckdb.connectionResult(conn -> {
            try(conn; reader; var arrowStream = ArrowArrayStream.allocateNew(allocator)) {
                checkState(conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
                Data.exportArrayStream(allocator, reader, arrowStream);
                var duckConn = conn.unwrap(DuckDBConnection.class);
                duckConn.registerArrowStream(name, arrowStream);
                return func.apply(DSL.using(duckConn));
            }
        });
    }
}
