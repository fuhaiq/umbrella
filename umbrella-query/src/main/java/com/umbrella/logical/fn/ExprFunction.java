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
            case Float f -> new LiteralFloat(f);
            case Double d -> new LiteralDouble(d);
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

    public static LogicalExpr alias(LogicalExpr expr, String name) {
        return as(expr, name);
    }

    public static LogicalExpr not(LogicalExpr expr) {
        return new UnaryExpr.Not(expr);
    }

    public static LogicalExpr and(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.And(l, r);
    }

    public static LogicalExpr or(LogicalExpr l, LogicalExpr r) {
        return new BooleanExpr.Or(l, r);
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

    public static LogicalExpr add(LogicalExpr l, LogicalExpr r) {
        return new MathExpr.Add(l, r);
    }

    public static LogicalExpr sub(LogicalExpr l, LogicalExpr r) {
        return new MathExpr.Subtract(l, r);
    }

    public static LogicalExpr div(LogicalExpr l, LogicalExpr r) {
        return new MathExpr.Divide(l, r);
    }

    public static LogicalExpr mul(LogicalExpr l, LogicalExpr r) {
        return new MathExpr.Multiply(l, r);
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

    public static LogicalExpr asc(LogicalExpr expr) {
        return new SortExpr.ASC(expr);
    }

    public static LogicalExpr desc(LogicalExpr expr) {
        return new SortExpr.DESC(expr);
    }

}
