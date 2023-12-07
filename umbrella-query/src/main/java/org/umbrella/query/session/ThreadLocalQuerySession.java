package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.umbrella.query.QueryEngine;

import java.util.HashMap;

@Slf4j
public class ThreadLocalQuerySession extends AbstractQuerySession {
    private final ThreadLocal<QuerySessionElement> threadLocal = new ThreadLocal<>();

    public ThreadLocalQuerySession(QueryEngine engine) {
        super(engine);
    }


    @Override
    protected QuerySessionElement getElement() {
        return threadLocal.get();
    }

    @Override
    protected void initElement() {
        var conn = engine.duckdb().configuration().connectionProvider().acquire();
        threadLocal.set(new QuerySessionElement(conn, new HashMap<>()));
    }
}
