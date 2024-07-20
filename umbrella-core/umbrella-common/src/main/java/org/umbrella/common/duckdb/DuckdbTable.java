package org.umbrella.common.duckdb;

import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.Collection;

public interface DuckdbTable {
    <T> void from(Class<T> clazz, Collection<T> collection);
    <T> void from(Class<T> clazz, T obj);
    void from(VectorSchemaRoot root);
}
