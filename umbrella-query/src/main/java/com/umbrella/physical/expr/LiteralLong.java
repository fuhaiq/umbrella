package com.umbrella.physical.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.commons.lang3.RandomStringUtils;

public record LiteralLong(Long n) implements PhysicalExpr {

    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        // Instantiate a BigIntVector. This doesn't allocate any memory for the data in vector.
//        return new BigIntVector(RandomStringUtils.randomAlphabetic(6), allocator);
        var vector = new BigIntVector(RandomStringUtils.randomAlphabetic(6), ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, n);
        }
        return vector;
    }
}
