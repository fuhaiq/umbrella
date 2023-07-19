package com.umbrella.physical.expr;

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;

public record LiteralString(String value) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        // Instantiate a VarCharVector. This doesn't allocate any memory for the data in vector.
//        return new VarCharVector(value, allocator);

        var vector = new VarCharVector(RandomStringUtils.randomAlphabetic(6), allocator);
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, value.getBytes(StandardCharsets.UTF_8));
        }
        return vector;
    }

    @Override
    public String toString() {
        return "'"+ value +"'";
    }
}
