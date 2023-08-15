package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.VectorBatch;
import com.umbrella.physical.arrow.expr.PhysicalExpr;

import java.util.List;

public class PhysicalProject extends AbstractPhysicalPlan {
    private final List<PhysicalExpr> expr;
    public PhysicalProject(PhysicalPlan input, List<PhysicalExpr> expr) {
        super(input);
        this.expr = expr;
    }

    @Override
    protected VectorBatch execute(VectorBatch input) {
        return VectorBatch.of(expr.stream().map(e -> e.evaluate(input)).toList());
    }

    @Override
    public String toString() {
        return "PhysicalProject{" +
                "expr=" + expr +
                '}';
    }
}
