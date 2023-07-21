package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.commons.lang3.RandomStringUtils;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }

    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        var vector = new BitVector(RandomStringUtils.randomAlphabetic(6), ExecutionContext.instance().allocator());
        switch (l) {
            case IntVector i -> {

            }
            default ->
        }
        for (var i = 0; i < l.getValueCount(); i++) {

        }
        return vector;
    }

    protected abstract boolean evaluate(Object l, Object r, ArrowType type);
}
