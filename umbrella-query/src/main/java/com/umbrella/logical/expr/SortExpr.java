package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public abstract class SortExpr implements LogicalExpr {

    protected final LogicalExpr expr;

    protected final String sort;

    protected SortExpr(LogicalExpr expr, String sort) {
        this.expr = expr;
        this.sort = sort;
    }

    @Override
    public Field toField(Schema schema) {
        var field = expr.toField(schema);
        return Field.notNullable(sort + "(" + field.getName() + ")", field.getType());
    }

    @Override
    public String toString() {
        return expr + "=" + sort;
    }

    public static class ASC extends SortExpr {
        public ASC(LogicalExpr expr) {
            super(expr, "ASC");
        }
    }

    public static class DESC extends SortExpr {
        public DESC(LogicalExpr expr) {
            super(expr, "DESC");
        }
    }
}
