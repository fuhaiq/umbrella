package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class AggExpr implements LogicalExpr {
    protected final String op;
    protected final LogicalExpr expr;

    protected AggExpr(String op, LogicalExpr expr) {
        this.op = op;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return op + "("+ expr.toString() +")";
    }

    @Override
    public Field toField(Schema schema) {
        var field = expr.toField(schema);
        return Field.notNullable(op + "(" + field.getName() + ")", field.getType());
    }

    public static class Sum extends AggExpr {
        public Sum(LogicalExpr expr) {
            super("SUM", expr);
        }
    }

    public static class Min extends AggExpr {
        public Min(LogicalExpr expr) {
            super("MIN", expr);
        }
    }

    public static class Max extends AggExpr {
        public Max(LogicalExpr expr) {
            super("MAX", expr);
        }
    }

    public static class Avg extends AggExpr {
        public Avg(LogicalExpr expr) {
            super("AVG", expr);
        }
    }

    public static class Count extends AggExpr {
        public Count(LogicalExpr expr) {
            super("COUNT", expr);
        }
        @Override
        public Field toField(Schema schema) {
            return Field.notNullable(op + "(" + expr.toField(schema).getName() + ")", new ArrowType.Int(64, true));
        }
    }
}
