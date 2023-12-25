package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.umbrella.query.EngineClient;

import java.sql.Connection;
import java.util.ArrayList;

@Slf4j
public class ThreadLocalEngineSession extends AbstractEngineSession {
    private final ThreadLocal<EngineSessionResource> threadLocal = new ThreadLocal<>();

    public ThreadLocalEngineSession(EngineClient client) {
        super(client);
    }

    @Override
    EngineSessionResource resource() {
        return threadLocal.get();
    }

    @Override
    void closeSession() {
        threadLocal.remove();
    }

    @Override
    void startSession(Connection conn) {
        threadLocal.set(new EngineSessionResource(conn, new ArrayList<>()));
    }
}
