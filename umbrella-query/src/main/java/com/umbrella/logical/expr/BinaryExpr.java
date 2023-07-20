package com.umbrella.logical.expr;

public abstract class BinaryExpr implements LogicalExpr {
    protected final String name;
    protected final String op;
    protected final LogicalExpr l;
    protected final LogicalExpr r;

    protected BinaryExpr(String name, String op, LogicalExpr l, LogicalExpr r) {
        this.name = name;
        this.op = op;
        this.l = l;
        this.r = r;
    }

    @Override
    public String toString() {
        return l.toString() + " " + op + " " + r.toString();
    }
}
