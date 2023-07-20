package com.umbrella.logical.expr;

public abstract class BinaryExpr implements LogicalExpr {
    protected final String op;
    protected final LogicalExpr l;
    protected final LogicalExpr r;

    protected BinaryExpr(String op, LogicalExpr l, LogicalExpr r) {
        this.op = op;
        this.l = l;
        this.r = r;
    }

    @Override
    public String toString() {
        return l.toString() + " " + op + " " + r.toString();
    }
}
