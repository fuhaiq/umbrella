package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class UnaryExpr implements LogicalExpr {
    protected final String op;

    protected final LogicalExpr expr;

    protected UnaryExpr(String op, LogicalExpr expr) {
        this.op = op;
        this.expr = expr;
    }

    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(op + " " + expr.toField(schema).getName(), ArrowType.PrimitiveType.Bool.INSTANCE);
    }

    @Override
    public String toString() {
        return op + " " + expr.toString();
    }

    public static class Not extends UnaryExpr {
        public Not(LogicalExpr expr) {
            super("NOT", expr);
        }
    }
}
