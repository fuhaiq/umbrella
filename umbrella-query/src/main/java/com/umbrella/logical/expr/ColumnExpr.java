package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public record ColumnExpr(String name) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return schema.findField(name);
    }

    @Override
    public String toString() {
        return "#" + name;
    }
}
