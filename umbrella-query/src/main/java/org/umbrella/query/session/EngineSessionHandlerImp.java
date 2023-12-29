package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.flight.FlightDescriptor;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.duckdb.DuckDBConnection;
import org.jooq.exception.DataAccessException;
import org.umbrella.query.EngineClient;
import org.umbrella.query.EngineHandlerImp;
import org.umbrella.query.reader.ArrowFlightStreamReader;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static cdjd.org.apache.arrow.util.Preconditions.checkState;

@Slf4j
public class EngineSessionHandlerImp extends EngineHandlerImp implements EngineSessionHandler {
    private final String name;
    private final EngineSessionResource resource;
    public EngineSessionHandlerImp(String name, EngineClient client, EngineSessionResource resource) {
        super(client);
        this.name = name;
        this.resource = resource;
    }
    @Override
    public void dremio(String sql) {
        var flightClient = client.flightClient();
        var auth = client.authFactory().getCredentialCallOption();
        var info = flightClient.getInfo(FlightDescriptor.command(sql.getBytes(StandardCharsets.UTF_8)), auth);
        var stream = flightClient.getStream(info.getEndpoints().get(0).getTicket(), auth);
        resource.addResource(stream); // 在 arrow 方法里面统一 addResource(reader)
        arrow(new ArrowFlightStreamReader(client.allocator(), stream));
    }
    @Override
    public void arrow(ArrowReader arrowReader) {
        try {
            var stream = ArrowArrayStream.allocateNew(client.allocator());
            resource.addResource(stream, arrowReader);
            checkState(resource.conn().isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            Data.exportArrayStream(client.allocator(), arrowReader, stream);
            var duckConn = resource.conn().unwrap(DuckDBConnection.class);
            duckConn.registerArrowStream(name, stream);
            if(log.isDebugEnabled()) log.debug("导出数据到 Arrow 会话完成.");
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }
}
