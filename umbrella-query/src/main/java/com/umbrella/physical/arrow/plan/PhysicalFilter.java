package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.VectorBatch;
import com.umbrella.physical.arrow.expr.BooleanExpr;
import com.umbrella.physical.arrow.expr.PhysicalExpr;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.VectorSchemaRoot;

import static com.google.common.base.Preconditions.checkState;

public class PhysicalFilter extends AbstractPhysicalPlan {
    private final PhysicalExpr expr;
    protected PhysicalFilter(PhysicalPlan input, PhysicalExpr expr) {
        super(input);
        checkState((expr instanceof BooleanExpr), "Expr type "+ expr.getClass().getName() +" is not supported in PhysicalFilter");
        this.expr = expr;
    }

    @Override
    protected VectorBatch execute(VectorBatch input) {
        var retSize = 0;
        var bool = (BitVector) expr.evaluate(input);
        checkState(bool.getValueCount() == input.rowCount, "数据条数不一致");
        var fieldsSize = input.rowCount;
        for (var i = 0; i < input.rowCount; i++) {
            for(var index = 0; index < fieldsSize; index++) {
                var f = input.getVector(index);
                checkState(bool.getValueCount() == f.getValueCount(), "数据条数不一致");
            }
            retSize += bool.get(i);
        }



        return null;
    }

    @Override
    public String toString() {
        return "PhysicalFilter{" +
                "expr=" + expr +
                '}';
    }
}
