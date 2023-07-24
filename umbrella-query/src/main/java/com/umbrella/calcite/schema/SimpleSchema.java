package com.umbrella.calcite.schema;

import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;

import java.util.Map;

public class SimpleSchema extends AbstractSchema {

    private final Map<String, Table> tables;

    private SimpleSchema(Map<String, Table> tables) {
        this.tables = tables;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return super.getTableMap();
    }
}
