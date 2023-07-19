package com.umbrella.physical.expr;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

public interface PhysicalExpr {

    BufferAllocator allocator = new RootAllocator();

    FieldVector evaluate(VectorSchemaRoot tabular);

}
