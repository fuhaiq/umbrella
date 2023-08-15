package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.BigIntVector;

public record LiteralLong(Long n) implements PhysicalExpr {

    @Override
    public BigIntVector evaluate(VectorBatch tabular) {
        var vector = new BigIntVector(n.toString(), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.setSafe(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
}
