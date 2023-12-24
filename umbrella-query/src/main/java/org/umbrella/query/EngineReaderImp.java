package org.umbrella.query;

import org.jooq.DSLContext;

public record EngineReaderImp(
        DSLContext duckdb,
        DSLContext dremio
) implements EngineReader {
    @Override
    public DSLContext duckdb() {
        return duckdb;
    }

    @Override
    public DSLContext dremio() {
        return dremio;
    }
}
