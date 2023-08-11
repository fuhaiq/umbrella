package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

public record LiteralBool(Boolean n) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var vector = new BitVector(n.toString(), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, n ? 1 : 0);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }

    @Override
    public String toString() {
        return "LiteralBool{" +
                "n=" + n +
                '}';
    }
}
