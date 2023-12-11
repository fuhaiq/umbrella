package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.umbrella.query.QueryEngine;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
public class ThreadLocalQuerySession extends AbstractQuerySession {
    private final ThreadLocal<QuerySessionElement> threadLocal = new ThreadLocal<>();

    public ThreadLocalQuerySession(QueryEngine engine) {
        super(engine);
    }


    @Override
    QuerySessionElement element() {
        return threadLocal.get();
    }

    @Override
    public void start() {
        checkState(element() == null, "开启 Arrow 会话失败,会话已经开启.");
        var conn = engine.duckdb().configuration().connectionProvider().acquire();
        threadLocal.set(new QuerySessionElement(conn, new HashMap<>()));
        if(log.isDebugEnabled()) log.debug("开启 Arrow 会话");
    }
}
