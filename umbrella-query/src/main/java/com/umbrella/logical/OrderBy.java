package com.umbrella.logical;

import com.umbrella.logical.expr.LogicalExpr;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public record OrderBy(LogicalPlan input, LogicalExpr... expr) implements LogicalPlan {
    @Override
    public Schema schema() {
        return input.schema();
    }

    @Override
    public List<LogicalPlan> children() {
        return List.of(input);
    }

    @Override
    public String toString() {
        return "OrderBy: " + StringUtils.join(expr, ",");
    }
}
