package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.expr.BooleanExpr;
import com.umbrella.physical.arrow.expr.PhysicalExpr;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.VectorSchemaRoot;

import java.util.BitSet;

import static com.google.common.base.Preconditions.checkState;

public class PhysicalFilter extends AbstractPhysicalPlan {
    private final PhysicalExpr expr;
    protected PhysicalFilter(PhysicalPlan input, PhysicalExpr expr) {
        super(input);
        checkState((expr instanceof BooleanExpr), "Expr type "+ expr.getClass().getName() +" is not supported in PhysicalFilter");
        this.expr = expr;
    }

    @Override
    protected VectorSchemaRoot execute(VectorSchemaRoot input) {
        var size = input.getRowCount();
        var bit = new BitSet(size);
        var bool = (BitVector) expr.evaluate(input);
        for (int i = 0; i < size; i++) {
            bit.set(i, bool.get(i) == 1);
        }
        var retSize = bit.cardinality();
        return null;
    }

    @Override
    public String toString() {
        return "PhysicalFilter{" +
                "expr=" + expr +
                '}';
    }
}
