package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public record LiteralString(String name) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(name, ArrowType.Utf8.INSTANCE);
    }

    @Override
    public String toString() {
        return "'"+ name +"'";
    }
}
