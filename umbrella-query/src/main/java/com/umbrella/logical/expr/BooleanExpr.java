package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(String op, LogicalExpr l, LogicalExpr r) {
        super(op, l, r);
    }

    @Override
    public Field toField(Schema schema) {
        var name = l.toField(schema).getName() + " " + op + " " + r.toField(schema).getName();
        return Field.notNullable(name, ArrowType.Bool.INSTANCE);
    }

    public static class And extends BooleanExpr {
        public And(LogicalExpr l, LogicalExpr r) {
            super("AND", l, r);
        }
    }

    public static class Or extends BooleanExpr {
        public Or(LogicalExpr l, LogicalExpr r) {
            super("OR", l, r);
        }
    }

    public static class Eq extends BooleanExpr {
        public Eq(LogicalExpr l, LogicalExpr r) {
            super("=", l, r);
        }
    }

    public static class Neq extends BooleanExpr {
        public Neq(LogicalExpr l, LogicalExpr r) {
            super("!=", l, r);
        }
    }

    public static class Gt extends BooleanExpr {
        public Gt(LogicalExpr l, LogicalExpr r) {
            super(">", l, r);
        }
    }

    public static class GtEq extends BooleanExpr {
        public GtEq(LogicalExpr l, LogicalExpr r) {
            super(">=", l, r);
        }
    }

    public static class Lt extends BooleanExpr {
        public Lt(LogicalExpr l, LogicalExpr r) {
            super("<", l, r);
        }
    }

    public static class LtEq extends BooleanExpr {
        public LtEq(LogicalExpr l, LogicalExpr r) {
            super("<=", l, r);
        }
    }
}
