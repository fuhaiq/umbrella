package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.VectorSchemaRoot;

public record LiteralDouble(Double n) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var vector = new Float8Vector(n.toString(), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
}
