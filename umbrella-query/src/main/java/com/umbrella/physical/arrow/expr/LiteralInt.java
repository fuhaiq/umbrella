package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.commons.lang3.RandomStringUtils;

public record LiteralInt(Integer n) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var vector = new IntVector(n.toString(), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
}
