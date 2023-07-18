package com.umbrella.logical.fn;

import com.umbrella.logical.expr.*;
import org.apache.arrow.vector.types.pojo.ArrowType;

public final class ExprFunction {

    private ExprFunction(){}

    public static LogicalExpr lit(Object value) {
        return switch (value) {
            case Integer i -> new LiteralInt(i);
            case Long l -> new LiteralLong(l);
            case String s -> new LiteralString(s);
            default -> throw new UnsupportedOperationException(value.getClass().getName() + " is not supported");
        };
    }

    public static LogicalExpr cast(LogicalExpr expr, ArrowType type) {
        return new CastExpr(expr, type);
    }

    public static LogicalExpr col(String name) {
        return new ColumnExpr(name);
    }

    public static LogicalExpr as(LogicalExpr expr, String name) {
        return new AsExpr(expr, name);
    }

    public static LogicalExpr cast(LogicalExpr expr, String name) {
        return as(expr, name);
    }

    public static LogicalExpr eq(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.Eq(l, r);
    }

    public static LogicalExpr neq(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.Neq(l, r);
    }

    public static LogicalExpr gt(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.Gt(l, r);
    }

    public static LogicalExpr lt(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.Lt(l, r);
    }

    public static LogicalExpr gteq(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.GtEq(l, r);
    }

    public static LogicalExpr lteq(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.LtEq(l, r);
    }

    public static LogicalExpr min(LogicalExpr expr) {
        return new AggExpr.Min(expr);
    }

    public static LogicalExpr max(LogicalExpr expr) {
        return new AggExpr.Max(expr);
    }

    public static LogicalExpr count(LogicalExpr expr) {
        return new AggExpr.Count(expr);
    }

    public static LogicalExpr sum(LogicalExpr expr) {
        return new AggExpr.Sum(expr);
    }

    public static LogicalExpr avg(LogicalExpr expr) {
        return new AggExpr.Avg(expr);
    }

}
