package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;


public interface PhysicalExpr {
    FieldVector evaluate(VectorBatch tabular);
}
