package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.commons.lang3.RandomStringUtils;

public record LiteralFloat(Float n) implements PhysicalExpr {

    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var vector = new Float4Vector(RandomStringUtils.randomAlphabetic(6), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
}
