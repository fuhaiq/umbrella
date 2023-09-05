package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.arrow.vector.types.Types.MinorType.*;

public record CastExpr(PhysicalExpr expr, Types.MinorType type) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        try(var res = expr.evaluate(tabular)){
            checkState(res.getMinorType() == INT
                    || res.getMinorType() == BIGINT
                    || res.getMinorType() == FLOAT4
                    || res.getMinorType() == FLOAT8, "Can't cast " + res.getMinorType() + "in Cast expression");
            if(res.getMinorType() == type) return res;
            var count = res.getValueCount();
            var ret = FieldVectorUtils.of(res.getName(), type, ExecutionContext.instance().allocator());
            FieldVectorUtils.allocateNew(ret, count);
            for (var i = 0; i < count; i++) {
                var value = (Number) res.getObject(i);
                if(type == INT) FieldVectorUtils.set(ret, i, value.intValue());
                else if(type == BIGINT) FieldVectorUtils.set(ret, i, value.longValue());
                else if(type == FLOAT4) FieldVectorUtils.set(ret, i, value.floatValue());
                else if(type == FLOAT8) FieldVectorUtils.set(ret, i, value.doubleValue());
                else throw new UnsupportedOperationException("Type "+ type +" is not supported in Cast expression");
            }
            ret.setValueCount(count);
            return ret;
        }
    }

    @Override
    public String toString() {
        return "CastExpr{" +
                "expr=" + expr +
                ", type=" + type +
                '}';
    }
}
