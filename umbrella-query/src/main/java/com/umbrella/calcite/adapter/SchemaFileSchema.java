package com.umbrella.calcite.adapter;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import java.util.Map;
import static com.google.common.base.Preconditions.*;

public class SchemaFileSchema extends AbstractSchema  {
    private final Map<String, Table> tableMap;

    private SchemaFileSchema(Map<String, Table> tableMap) {
        this.tableMap = tableMap;
    }

    @Override protected Map<String, Table> getTableMap() {
        return tableMap;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();

        public Builder addTable(String name, SchemaFileTable table) {
            checkState(!Strings.isNullOrEmpty(name), "Table 名字不能为空");
            checkNotNull(table, "Table 不能为空");
            builder.put(name, table);
            return this;
        }

        public SchemaFileSchema build() {
            return new SchemaFileSchema(builder.build());
        }
    }

}
