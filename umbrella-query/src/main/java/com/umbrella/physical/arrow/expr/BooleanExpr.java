package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.arrow.vector.types.Types.MinorType.*;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }
    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        checkState(l.getMinorType() == INT
                || l.getMinorType() == BIGINT
                || l.getMinorType() == FLOAT4
                || l.getMinorType() == FLOAT8
                || l.getMinorType() == BIT, l.getMinorType() + " is not supported in bool expression");
        checkState(r.getMinorType() == INT
                || r.getMinorType() == BIGINT
                || r.getMinorType() == FLOAT4
                || r.getMinorType() == FLOAT8
                || r.getMinorType() == BIT, r.getMinorType() + " is not supported in bool expression");
        var vector = new BitVector(l.getName() + r.getName(), ExecutionContext.instance().allocator());
        vector.allocateNew(l.getValueCount());
        for (var index = 0; index < l.getValueCount(); index++) {
            if(l.getMinorType() == BIT && r.getMinorType() == BIT) {
                var lValue = (Boolean) l.getObject(index);
                var rValue = (Boolean) r.getObject(index);
                vector.set(index, evaluate(lValue, rValue) ? 1 : 0);
            } else {
                var lValue = (Number) l.getObject(index);
                var rValue = (Number) r.getObject(index);
                vector.set(index, evaluate(lValue, rValue) ? 1 : 0);
            }
        }
        return vector;
    }
    protected abstract boolean evaluate(Number l, Number r);

    protected abstract boolean evaluate(Boolean l, Boolean r);

    public static class And extends BooleanExpr {
        public And(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Number l, Number r) {
            return l.intValue() == 1 && r.intValue() == 1;
        }

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            return l && r;
        }

        @Override
        public String toString() {
            return "And{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            return l || r;
        }

        @Override
        public String toString() {
            return "Or{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            if(l && r) return true;
            else return !l && !r;
        }

        @Override
        public String toString() {
            return "Eq{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            if(l && r) return false;
            else return l || r;
        }

        @Override
        public String toString() {
            return "Neq{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            throw new UnsupportedOperationException("Gt is not supported in Bool expression");
        }

        @Override
        public String toString() {
            return "Gt{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            throw new UnsupportedOperationException("Ge is not supported in Bool expression");
        }

        @Override
        public String toString() {
            return "Ge{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            throw new UnsupportedOperationException("Lt is not supported in Bool expression");
        }

        @Override
        public String toString() {
            return "Lt{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
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

        @Override
        protected boolean evaluate(Boolean l, Boolean r) {
            throw new UnsupportedOperationException("Le is not supported in Bool expression");
        }

        @Override
        public String toString() {
            return "Le{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
        }
    }
}
