package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class MathExpr extends BinaryExpr {
    protected MathExpr(String op, LogicalExpr l, LogicalExpr r) {
        super(op, l, r);
    }

    @Override
    public Field toField(Schema schema) {
        var ll = l.toField(schema);
        var name = ll.getName() + " " + op + " " + r.toField(schema).getName();
        return Field.notNullable(name, ll.getType());
    }

    public static class Add extends MathExpr {
        public Add(LogicalExpr l, LogicalExpr r) {
            super("+", l, r);
        }
    }

    public static class Subtract extends MathExpr {
        public Subtract(LogicalExpr l, LogicalExpr r) {
            super("-", l, r);
        }
    }
    public static class Multiply extends MathExpr {
        public Multiply(LogicalExpr l, LogicalExpr r) {
            super("*", l, r);
        }
    }
    public static class Divide extends MathExpr {
        public Divide(LogicalExpr l, LogicalExpr r) {
            super("/", l, r);
        }
    }
}
