package org.umbrella.query.session;

import org.jooq.DSLContext;

import java.io.Closeable;

public interface EngineSession extends Closeable {
    void start();
    DSLContext dsl();
    EngineSessionHandler define(String name);
    @Override
    void close();
}
