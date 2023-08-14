package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;

import java.nio.charset.StandardCharsets;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public record CastExpr(PhysicalExpr expr, Types.MinorType type) implements PhysicalExpr {
    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var res = expr.evaluate(tabular);
        var size = res.getValueCount();
        if(type == INT) {
            var vector = new IntVector(res.getName(), ExecutionContext.instance().allocator());
            vector.allocateNew(size);
            for (var i = 0; i < size; i++) {
                vector.setSafe(i, (int) res.getObject(i));
            }
            vector.setValueCount(size);
            return vector;
        } else if (type == BIGINT) {
            var vector = new BigIntVector(res.getName(), ExecutionContext.instance().allocator());
            vector.allocateNew(size);
            for (var i = 0; i < size; i++) {
                vector.setSafe(i, (long) res.getObject(i));
            }
            vector.setValueCount(size);
            return vector;
        } else if (type == FLOAT4) {
            var vector = new Float4Vector(res.getName(), ExecutionContext.instance().allocator());
            vector.allocateNew(size);
            for (var i = 0; i < size; i++) {
                vector.setSafe(i, (float) res.getObject(i));
            }
            vector.setValueCount(size);
            return vector;
        } else if (type == FLOAT8) {
            var vector = new Float8Vector(res.getName(), ExecutionContext.instance().allocator());
            vector.allocateNew(size);
            for (var i = 0; i < size; i++) {
                vector.setSafe(i, (double) res.getObject(i));
            }
            vector.setValueCount(size);
            return vector;
        } else if (type == VARCHAR) {
            var vector = new VarCharVector(res.getName(), ExecutionContext.instance().allocator());
            vector.allocateNew(size);
            for (var i = 0; i < size; i++) {
                vector.setSafe(i, res.getObject(i).toString().getBytes(StandardCharsets.UTF_8));
            }
            vector.setValueCount(size);
            return vector;
        } else {
            throw new UnsupportedOperationException("Type "+ type +" is not supported in CastExpr");
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
