package com.umbrella.physical.expr;

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

public interface PhysicalExpr {

    FieldVector evaluate(VectorSchemaRoot tabular);

}
