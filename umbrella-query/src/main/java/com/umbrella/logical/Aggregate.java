package com.umbrella.logical;

import com.umbrella.logical.expr.AggExpr;
import com.umbrella.logical.expr.LogicalExpr;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

import java.util.ArrayList;
import java.util.List;

public record Aggregate(LogicalPlan input, List<LogicalExpr> groupExpr, List<AggExpr> aggregateExpr) implements LogicalPlan {
    @Override
    public Schema schema() {
        var fields = new ArrayList<Field>();
        groupExpr.stream().map(it -> it.toField(input.schema())).forEach(fields::add);
        aggregateExpr.stream().map(it -> it.toField(input.schema())).forEach(fields::add);
        return new Schema(fields);
    }

    @Override
    public List<LogicalPlan> children() {
        return List.of(input);
    }

    @Override
    public String toString() {
        return "Aggregate: groupExpr=" + groupExpr.toString() + ", aggregateExpr=" + aggregateExpr.toString();
    }
}
