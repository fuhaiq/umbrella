package org.umbrella.query;

import org.apache.arrow.memory.BufferAllocator;
import org.jooq.DSLContext;

public class QueryEngine {
    public final DSLContext mysql;
    public final DSLContext duckdb;
    final BufferAllocator allocator;
    public QueryEngine(DSLContext mysql, DSLContext duckdb, BufferAllocator allocator) {
        this.mysql = mysql;
        this.duckdb = duckdb;
        this.allocator = allocator;
    }

    public QueryExecutor with(String name) {
        return new QueryExecutor(name, this);
    }
}
