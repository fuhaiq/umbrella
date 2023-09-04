package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.arrow.vector.types.Types.MinorType.*;
import static org.apache.arrow.vector.types.Types.MinorType.FLOAT8;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }
    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        checkState(l.getMinorType() == INT
                || l.getMinorType() == BIGINT
                || l.getMinorType() == FLOAT4
                || l.getMinorType() == FLOAT8, l.getMinorType() + "is not supported in bool expression");
        checkState(r.getMinorType() == INT
                || r.getMinorType() == BIGINT
                || r.getMinorType() == FLOAT4
                || r.getMinorType() == FLOAT8, r.getMinorType() + "is not supported in bool expression");
        var vector = new BitVector(l.getName() + r.getName(), ExecutionContext.instance().allocator());
        vector.allocateNew(l.getValueCount());
        for (var index = 0; index < l.getValueCount(); index++) {
            var lValue = (Number) l.getObject(index);
            var rValue = (Number) r.getObject(index);
            vector.set(index, evaluate(lValue, rValue) ? 1 : 0);
        }
        return vector;
    }
    protected abstract boolean evaluate(Number l, Number r);

    public static class And extends BooleanExpr {
        public And(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.intValue() == 1 && r.intValue() == 1;
        }
    }

    public static class Or extends BooleanExpr {
        public Or(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.intValue() == 1 || r.intValue() == 1;
        }
    }

    public static class Eq extends BooleanExpr {
        public Eq(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.floatValue() == r.floatValue();
        }
    }

    public static class Neq extends BooleanExpr {
        public Neq(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.floatValue() != r.floatValue();
        }
    }

    public static class Gt extends BooleanExpr {
        public Gt(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.floatValue() > r.floatValue();
        }
    }

    public static class Ge extends BooleanExpr {
        public Ge(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.floatValue() >= r.floatValue();
        }
    }

    public static class Lt extends BooleanExpr {
        public Lt(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.floatValue() < r.floatValue();
        }
    }

    public static class Le extends BooleanExpr {
        public Le(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.floatValue() <= r.floatValue();
        }
    }
}
