package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.VectorSchemaRoot;

public record LiteralDouble(Double n) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var vector = new Float8Vector(n.toString(), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.setSafe(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }

    @Override
    public String toString() {
        return "LiteralDouble{" +
                "n=" + n +
                '}';
    }
}
