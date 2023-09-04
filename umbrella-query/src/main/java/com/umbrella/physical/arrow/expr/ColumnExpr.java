package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.util.VectorBatchAppender;

public record ColumnExpr(int i) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var vector = tabular.getVector(i);
        var ret = FieldVectorUtils.of(vector.getField(), ExecutionContext.instance().allocator());
        FieldVectorUtils.allocateNew(ret, vector.getValueCount());
        VectorBatchAppender.batchAppend(ret, vector);
        return ret;
    }

    @Override
    public String toString() {
        return "#" + i;
    }
}
