package com.umbrella.calcite;

import com.google.common.collect.ImmutableMap;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.schema.SchemaPlus;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SchemaMapDataContext implements DataContext {

    private final ImmutableMap<String, ?> map;

    private final SchemaPlus schema;

    public SchemaMapDataContext(Map<String, ?> map, SchemaPlus schema) {
        this.map = ImmutableMap.copyOf(checkNotNull(map, "Map 不能为空"));
        this.schema = checkNotNull(schema, "Schema 不能为空");
    }

    public SchemaMapDataContext(SchemaPlus schema) {
        this(Map.of(Variable.CANCEL_FLAG.camelName, new AtomicBoolean(false)), schema);
    }

    @Override
    public SchemaPlus getRootSchema() {
        return schema;
    }

    @Override
    public JavaTypeFactory getTypeFactory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryProvider getQueryProvider() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Object get(String name) {
        return map.get(name);
    }
}
