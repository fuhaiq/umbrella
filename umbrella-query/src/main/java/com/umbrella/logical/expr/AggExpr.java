package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class AggExpr implements LogicalExpr {
    protected final String name;
    protected final LogicalExpr expr;

    protected AggExpr(String name, LogicalExpr expr) {
        this.name = name;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return name + "("+ expr.toString() +")";
    }

    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(name, expr.toField(schema).getType());
    }

    public static class Sum extends AggExpr {
        public Sum(LogicalExpr expr) {
            super("sum", expr);
        }
    }

    public static class Min extends AggExpr {
        public Min(LogicalExpr expr) {
            super("min", expr);
        }
    }

    public static class Max extends AggExpr {
        public Max(LogicalExpr expr) {
            super("max", expr);
        }
    }

    public static class Avg extends AggExpr {
        public Avg(LogicalExpr expr) {
            super("avg", expr);
        }
    }

    public static class Count extends AggExpr {
        public Count(LogicalExpr expr) {
            super("count", expr);
        }
        @Override
        public Field toField(Schema schema) {
            return Field.notNullable(name, new ArrowType.Int(64, true));
        }
    }
}
