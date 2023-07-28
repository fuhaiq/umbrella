package com.umbrella.calcite.adapter;

import com.google.common.base.Strings;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;


import java.util.HashMap;
import java.util.Map;
import static com.google.common.base.Preconditions.*;

public class SchemaBasedFileSchema extends AbstractSchema {

    public String getName() {
        return name;
    }
    private final String name;

    private final Map<String, Table> tableMap = new HashMap<>();

    public SchemaBasedFileSchema(String name) {
        this.name = name;
    }

    public SchemaBasedFileSchema addTable(String name, SchemaBasedFileTable table) {
        checkState(!Strings.isNullOrEmpty(name), "Table 名字不能为空");
        checkNotNull(table, "Table 不能为空");
        checkState(!tableMap.containsKey(name), "Table " + name + " 已经存在");
        tableMap.put(name, table);
        return this;
    }

    @Override protected Map<String, Table> getTableMap() {
        return tableMap;
    }
}
