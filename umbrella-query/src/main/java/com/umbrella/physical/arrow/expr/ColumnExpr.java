package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.util.VectorBatchAppender;

public record ColumnExpr(int i) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        FieldVector ret;
        var v = tabular.getVector(i);
        switch (v.getMinorType()) {
            case INT -> ret = new IntVector(v.getField(), ExecutionContext.instance().allocator());
            case BIGINT -> ret = new BigIntVector(v.getField(), ExecutionContext.instance().allocator());
            case FLOAT4 -> ret = new Float4Vector(v.getField(), ExecutionContext.instance().allocator());
            case FLOAT8 -> ret = new Float8Vector(v.getField(), ExecutionContext.instance().allocator());
            case VARCHAR -> ret = new VarCharVector(v.getField(), ExecutionContext.instance().allocator());
            case BIT -> ret = new BitVector(v.getField(), ExecutionContext.instance().allocator());
            case DECIMAL -> ret = new DecimalVector(v.getField(), ExecutionContext.instance().allocator());
            case default -> throw new UnsupportedOperationException("Vector type "+ v.getMinorType() +" is not supported");
        }
        VectorBatchAppender.batchAppend(ret, v);
        return ret;
    }

    @Override
    public String toString() {
        return "#" + i;
    }
}
