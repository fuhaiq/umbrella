package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public record LiteralDouble(Double n) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(n.toString(), new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE));
    }

    @Override
    public String toString() {
        return n.toString();
    }
}
