package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

public record CastExpr(PhysicalExpr expr, Types.MinorType type) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var res = expr.evaluate(tabular);
        var count = res.getValueCount();
        var ret = FieldVectorUtils.of(res.getName(), type, ExecutionContext.instance().allocator());
        FieldVectorUtils.allocateNew(ret, count);
        for (var i = 0; i < count; i++) {
            FieldVectorUtils.castAndSet(ret, i, res.getObject(i));
        }
        ret.setValueCount(count);
        return ret;
    }

    @Override
    public String toString() {
        return "CastExpr{" +
                "expr=" + expr +
                ", type=" + type +
                '}';
    }
}
