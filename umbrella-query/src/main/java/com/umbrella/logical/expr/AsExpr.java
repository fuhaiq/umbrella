package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public record AsExpr(LogicalExpr expr, String name) implements LogicalExpr {

    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(name, expr.toField(schema).getType());
    }

    @Override
    public String toString() {
        return expr.toString() + " as " + name;
    }
}
