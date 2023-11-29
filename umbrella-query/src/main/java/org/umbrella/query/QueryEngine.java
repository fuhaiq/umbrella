package org.umbrella.query;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.apache.arrow.memory.BufferAllocator;
import org.jooq.DSLContext;

import static com.google.common.base.Preconditions.checkState;

@RequiredArgsConstructor
public class QueryEngine {
    private final ImmutableMap<String, DSLContext> contexts;
    public final DSLContext duckdb;
    final BufferAllocator allocator;

    public QueryExecutor with(String name) {
        return new QueryExecutor(name, this);
    }

    public DSLContext db(String key) {
        checkState(contexts.containsKey(key), Strings.lenientFormat("没有 %s 数据库连接", key));
        return contexts.get(key);
    }
}
