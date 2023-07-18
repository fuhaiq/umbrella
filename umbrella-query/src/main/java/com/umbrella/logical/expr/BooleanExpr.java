package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(String name, String op, LogicalExpr l, LogicalExpr r) {
        super(name, op, l, r);
    }

    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(name, ArrowType.PrimitiveType.Bool.INSTANCE);
    }

    public static class Eq extends BooleanExpr {
        public Eq(LogicalExpr l, LogicalExpr r) {
            super("eq", "=", l, r);
        }
    }

    public static class Neq extends BooleanExpr {
        public Neq(LogicalExpr l, LogicalExpr r) {
            super("neq", "!=", l, r);
        }
    }

    public static class Gt extends BooleanExpr {
        public Gt(LogicalExpr l, LogicalExpr r) {
            super("gt", ">", l, r);
        }
    }

    public static class GtEq extends BooleanExpr {
        public GtEq(LogicalExpr l, LogicalExpr r) {
            super("gteq", ">=", l, r);
        }
    }

    public static class Lt extends BooleanExpr {
        public Lt(LogicalExpr l, LogicalExpr r) {
            super("lt", "<", l, r);
        }
    }

    public static class LtEq extends BooleanExpr {
        public LtEq(LogicalExpr l, LogicalExpr r) {
            super("lteq", "<=", l, r);
        }
    }
}
