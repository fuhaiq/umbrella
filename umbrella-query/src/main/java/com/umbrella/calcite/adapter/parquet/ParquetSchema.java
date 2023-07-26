package com.umbrella.calcite.adapter.parquet;

import com.google.common.collect.ImmutableMap;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.impl.AbstractSchema;
import java.util.Map;

public class ParquetSchema extends AbstractSchema  {
    private Map<String, Table> tableMap;

    @Override protected Map<String, Table> getTableMap() {
        if (tableMap == null) {
            var builder = ImmutableMap.builder();
        }
        return tableMap;
    }

}
