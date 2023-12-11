package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.umbrella.query.QueryEngine;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
public class SimpleQuerySession extends AbstractQuerySession {
    private QuerySessionElement element;

    public SimpleQuerySession(QueryEngine engine) {
        super(engine);
    }

    @Override
    QuerySessionElement element() {
        return element;
    }

    @Override
    public void start() {
        checkState(element() == null, "开启 Arrow 会话失败,会话已经开启.");
        var conn = engine.duckdb().configuration().connectionProvider().acquire();
        element = new QuerySessionElement(conn, new HashMap<>());
        if(log.isDebugEnabled()) log.debug("开启 Arrow 会话");
    }
}
