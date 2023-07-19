package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class MathExpr extends BinaryExpr {
    protected MathExpr(String name, String op, LogicalExpr l, LogicalExpr r) {
        super(name, op, l, r);
    }

    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(name, l.toField(schema).getType());
    }

    public static class Add extends MathExpr {
        public Add(LogicalExpr l, LogicalExpr r) {
            super("add", "+", l, r);
        }
    }

    public static class Subtract extends MathExpr {
        public Subtract(LogicalExpr l, LogicalExpr r) {
            super("sub", "-", l, r);
        }
    }
    public static class Multiply extends MathExpr {
        public Multiply(LogicalExpr l, LogicalExpr r) {
            super("mul", "*", l, r);
        }
    }
    public static class Divide extends MathExpr {
        public Divide(LogicalExpr l, LogicalExpr r) {
            super("div", "/", l, r);
        }
    }
}
