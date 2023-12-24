package org.umbrella.query;

import org.jooq.DSLContext;

public record EngineWriterImp(
        DSLContext mysql
) implements EngineWriter {
    @Override
    public DSLContext mysql() {
        return mysql;
    }
}
