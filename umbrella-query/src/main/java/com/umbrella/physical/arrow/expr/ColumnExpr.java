package com.umbrella.physical.arrow.expr;

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

public record ColumnExpr(int i) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        return tabular.getVector(i);
    }

    @Override
    public String toString() {
        return "#" + i;
    }
}
