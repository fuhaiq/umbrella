package org.apache.calcite.adapter.arrow;

import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import static com.google.common.base.Preconditions.*;
import java.util.HashMap;
import java.util.Map;

public class ArrowSchema extends AbstractSchema {
    private final String name;
    private final Map<String, Table> tableMap;

    private ArrowSchema(String name, Map<String, Table> tableMap) {
        this.name = name;
        this.tableMap = tableMap;
    }

    @Override
    protected Map<String, Table> getTableMap() {
        return tableMap;
    }

    public String getName() {
        return name;
    }

    public static Builder newBuilder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private final Map<String, Table> tableMap = new HashMap<>();

        public Builder(String name) {
            this.name = checkNotNull(name, "Schema name is null");
        }

        public Builder addTable(String name, ArrowTable table) {
            checkState(!tableMap.containsKey(name), "Table "+ name +" is already exists");
            tableMap.put(name, table);
            return this;
        }

        public ArrowSchema build() {
            return new ArrowSchema(name, tableMap);
        }
    }
}
