package com.umbrella.logical;

import com.umbrella.logical.expr.LogicalExpr;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record Projection(LogicalPlan input, LogicalExpr... expr) implements LogicalPlan {
    @Override
    public Schema schema() {
        var fields = Arrays.stream(expr).map(it -> it.toField(input.schema())).collect(Collectors.toList());
        return new Schema(fields);
    }

    @Override
    public List<LogicalPlan> children() {
        return List.of(input);
    }

    @Override
    public String toString() {
        return "Projection: " + StringUtils.join(expr, ",");
    }
}
