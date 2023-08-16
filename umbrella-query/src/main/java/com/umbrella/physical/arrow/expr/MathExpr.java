package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

import java.math.BigDecimal;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public abstract class MathExpr extends BinaryExpr {
    protected MathExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }

    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r, Types.MinorType type) {
        var vector = FieldVectorUtils.of(l.getName() + r.getName(), type, ExecutionContext.instance().allocator());
        FieldVectorUtils.allocateNew(vector, l.getValueCount());
        for (var index = 0; index < l.getValueCount(); index++) {
            var ll = l.getObject(index);
            var rr = r.getObject(index);
            if(ll instanceof BigDecimal lb && rr instanceof BigDecimal rb && type == DECIMAL) {
                FieldVectorUtils.set(vector, index, evaluate(lb, rb));
            } else if (ll instanceof Number ln && rr instanceof Number rn) {
                FieldVectorUtils.set(vector, index, evaluate(ln, rn, type));
            } else {
                throw new UnsupportedOperationException("Class "+ l.getClass().getName() +" is not supported in Math expression");
            }
        }
        return vector;
    }

    protected abstract Number evaluate(Number l, Number r, Types.MinorType type);

    protected abstract BigDecimal evaluate(BigDecimal l, BigDecimal r);


    public static class Add extends MathExpr {
        public Add(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }

        @Override
        protected Number evaluate(Number l, Number r, Types.MinorType type) {
            return switch (type) {
                case INT -> l.intValue() + r.intValue();
                case BIGINT -> l.longValue() + r.longValue();
                case FLOAT4 -> l.floatValue() + r.floatValue();
                case FLOAT8 -> l.doubleValue() + r.doubleValue();
                case default, null -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            return l.add(r);
        }

        @Override
        public String toString() {
            return "Add{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
        }
    }

    public static class Sub extends MathExpr {
        public Sub(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }

        @Override
        protected Number evaluate(Number l, Number r, Types.MinorType type) {
            return switch (type) {
                case INT -> l.intValue() - r.intValue();
                case BIGINT -> l.longValue() - r.longValue();
                case FLOAT4 -> l.floatValue() - r.floatValue();
                case FLOAT8 -> l.doubleValue() - r.doubleValue();
                case default, null -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            return l.subtract(r);
        }

        @Override
        public String toString() {
            return "Sub{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
        }
    }

    public static class Mul extends MathExpr {
        public Mul(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }

        @Override
        protected Number evaluate(Number l, Number r, Types.MinorType type) {
            return switch (type) {
                case INT -> l.intValue() * r.intValue();
                case BIGINT -> l.longValue() * r.longValue();
                case FLOAT4 -> l.floatValue() * r.floatValue();
                case FLOAT8 -> l.doubleValue() * r.doubleValue();
                case default, null -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            return l.multiply(r);
        }

        @Override
        public String toString() {
            return "Mul{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
        }
    }

    public static class Div extends MathExpr {
        public Div(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }

        @Override
        protected Number evaluate(Number l, Number r, Types.MinorType type) {
            return switch (type) {
                case INT -> l.intValue() / r.intValue();
                case BIGINT -> l.longValue() / r.longValue();
                case FLOAT4 -> l.floatValue() / r.floatValue();
                case FLOAT8 -> l.doubleValue() / r.doubleValue();
                case default, null -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            return l.divide(r);
        }

        @Override
        public String toString() {
            return "Div{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
        }
    }

    public static class Mod extends MathExpr {
        public Mod(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }

        @Override
        protected Number evaluate(Number l, Number r, Types.MinorType type) {
            return switch (type) {
                case INT -> l.intValue() % r.intValue();
                case BIGINT -> l.longValue() % r.longValue();
                case FLOAT4 -> l.floatValue() % r.floatValue();
                case FLOAT8 -> l.doubleValue() % r.doubleValue();
                case default, null -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            throw new UnsupportedOperationException("Mod is not supported for Decimal");
        }

        @Override
        public String toString() {
            return "Mod{" +
                    "l=" + l +
                    ", r=" + r +
                    '}';
        }
    }
}
