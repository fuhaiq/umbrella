package org.umbrella.common.duckdb;

import java.io.Closeable;

public interface DuckdbSession extends Closeable {
    void start();

    <T> T mapper(Class<T> mapper);

    DuckdbTable define(String name);

    @Override
    void close();
}
