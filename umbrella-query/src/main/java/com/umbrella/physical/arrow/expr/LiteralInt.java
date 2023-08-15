package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;

public record LiteralInt(Integer n) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var vector = new IntVector(n.toString(), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.setSafe(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
}
