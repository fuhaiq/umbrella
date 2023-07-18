package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public record CastExpr(LogicalExpr expr, ArrowType type) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(expr.toField(schema).getName(), type);
    }

    @Override
    public String toString() {
        return "CAST("+ expr.toString() +" AS "+ type.toString() +")";
    }
}
