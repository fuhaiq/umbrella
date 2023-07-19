package com.umbrella.physical.expr;

import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.commons.lang3.RandomStringUtils;

public record LiteralFloat(Float n) implements PhysicalExpr {

    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        // Instantiate a Float4Vector. This doesn't allocate any memory for the data in vector.
//        return new Float4Vector(RandomStringUtils.randomAlphabetic(6), allocator);
        var vector = new Float4Vector(RandomStringUtils.randomAlphabetic(6), allocator);
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, n);
        }
        return vector;
    }

    @Override
    public String toString() {
        return n.toString();
    }

}
