package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

public interface PhysicalExpr {

    FieldVector evaluate(VectorBatch tabular);

}
