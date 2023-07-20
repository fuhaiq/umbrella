package com.umbrella.logical;

import org.apache.arrow.vector.types.pojo.Schema;

import java.util.List;

public record Limit(LogicalPlan input, int n) implements LogicalPlan {
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
        return "Limit: " + n;
    }
}
