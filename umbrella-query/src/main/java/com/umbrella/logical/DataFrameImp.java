package com.umbrella.logical;

import com.umbrella.logical.DataFrame;
import com.umbrella.logical.LogicalPlan;
import com.umbrella.logical.expr.AggExpr;
import com.umbrella.logical.expr.LogicalExpr;
import org.apache.arrow.vector.types.pojo.Schema;


import java.util.List;

public record DataFrameImp(LogicalPlan input) implements DataFrame {
    @Override
    public DataFrame select(LogicalExpr... expr) {
        return new DataFrameImp(new Projection(input, expr));
    }

    @Override
    public DataFrame filter(LogicalExpr expr) {
        return new DataFrameImp(new Selection(input, expr));
    }

    @Override
    public DataFrame where(LogicalExpr expr) {
        return filter(expr);
    }

    @Override
    public DataFrame agg(List<LogicalExpr> groupBy, List<AggExpr> aggExpr) {
        return new DataFrameImp(new Aggregate(input, groupBy, aggExpr));
    }

    @Override
    public Schema schema() {
        return input.schema();
    }

    @Override
    public void explain() {
        System.out.println(format(input, 0));
    }

    private String format(LogicalPlan plan, int indent) {
        var b = new StringBuilder();
        for (var i = 0; i < indent; i++) {
            b.append("\t");
        }
        b.append(plan.toString()).append("\n");
        plan.children().forEach(it -> b.append(format(it, indent + 1)));
        return b.toString();
    }
}
