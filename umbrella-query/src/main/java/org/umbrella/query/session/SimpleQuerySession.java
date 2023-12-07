package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.umbrella.query.QueryEngine;

import java.util.HashMap;

@Slf4j
public class SimpleQuerySession extends AbstractQuerySession {
    protected QuerySessionElement element;

    public SimpleQuerySession(QueryEngine engine) {
        super(engine);
    }

    @Override
    protected QuerySessionElement getElement() {
        return element;
    }

    @Override
    protected void initElement() {
        var conn = engine.duckdb().configuration().connectionProvider().acquire();
        element = new QuerySessionElement(conn, new HashMap<>());
    }
}
