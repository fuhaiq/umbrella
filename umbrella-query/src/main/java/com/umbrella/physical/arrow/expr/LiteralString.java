package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.commons.lang3.RandomStringUtils;

import java.nio.charset.StandardCharsets;

public record LiteralString(String value) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var vector = new VarCharVector(value, ExecutionContext.instance().allocator());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.set(i, value.getBytes(StandardCharsets.UTF_8));
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
}
