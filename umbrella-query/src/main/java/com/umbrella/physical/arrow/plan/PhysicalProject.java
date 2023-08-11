package com.umbrella.physical.arrow.plan;

import com.google.common.collect.Iterables;
import com.umbrella.physical.arrow.expr.PhysicalExpr;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.List;

public class PhysicalProject extends AbstractPhysicalPlan {
    private final List<PhysicalExpr> expr;
    protected PhysicalProject(PhysicalPlan input, List<PhysicalExpr> expr) {
        super(input);
        this.expr = expr;
    }

    @Override
    protected VectorSchemaRoot execute(VectorSchemaRoot input) {
        var fields = Iterables.toArray(expr.stream().map(e -> e.evaluate(input)).toList(), FieldVector.class);
        return VectorSchemaRoot.of(fields);
    }

    @Override
    public String toString() {
        return "PhysicalProject{" +
                "expr=" + expr +
                '}';
    }
}
