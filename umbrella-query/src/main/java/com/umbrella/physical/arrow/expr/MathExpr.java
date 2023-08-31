package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import org.apache.arrow.vector.DecimalVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.javatuples.Pair;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

public abstract class MathExpr extends BinaryExpr {
    protected MathExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }
    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        Types.MinorType type = l.getMinorType();
        ArrowType arrowType = l.getField().getType();
        FieldVector greater = null;
        FieldVector lesser = null;
        Pair<Integer, Integer> pair;
        if(l.getMinorType().compareTo(r.getMinorType()) > 0) {
            type = l.getMinorType();
            arrowType = l.getField().getType();
            greater = l;
            lesser = r;
        } else if(l.getMinorType().compareTo(r.getMinorType()) < 0) {
            type = r.getMinorType();
            arrowType = r.getField().getType();
            greater = r;
            lesser = l;
        }

        if(greater != null) {
            var field = Field.notNullable(l.getName() + r.getName(), arrowType);
            FieldVector vector;
            if(type == Types.MinorType.DECIMAL) {
                var scale = ((ArrowType.Decimal) arrowType).getScale();
                vector = new DecimalVector(l.getName() + r.getName(), ExecutionContext.instance().allocator(), MathContext.DECIMAL64.getPrecision(), scale);
            } else {
                vector = FieldVectorUtils.of(field, type, ExecutionContext.instance().allocator());
            }
            FieldVectorUtils.allocateNew(vector, l.getValueCount());
            for (var index = 0; index < l.getValueCount(); index++) {
                var ll = greater.getObject(index);
                var rr = lesser.getObject(index);
                if(ll instanceof Number ln && rr instanceof Number rn) {
                    if(ln instanceof BigDecimal lb) {
                        FieldVectorUtils.set(vector, index, evaluate(lb, BigDecimal.valueOf(rn.longValue())));
                    } else {
                        FieldVectorUtils.set(vector, index, evaluate(ln, FieldVectorUtils.cast(rn, type, 0), type));
                    }
                } else {
                    throw new UnsupportedOperationException("Class "+ l.getClass().getName() +" is not supported in Math expression");
                }
            }
            return vector;
        } else if ((pair = FieldVectorUtils.compareTo(l.getField().getType(), r.getField().getType())) != null) {
            var vector = new DecimalVector(l.getName() + r.getName(), ExecutionContext.instance().allocator(), MathContext.DECIMAL64.getPrecision(), pair.getValue1());
            vector.allocateNew(l.getValueCount());
            for (var index = 0; index < l.getValueCount(); index++) {
                var ll = (BigDecimal) l.getObject(index);
                var rr = (BigDecimal) r.getObject(index);
                vector.set(index, evaluate(ll, rr));
            }
            return vector;
        } else {
            var field = Field.notNullable(l.getName() + r.getName(), l.getField().getType());
            var vector = FieldVectorUtils.of(field, l.getMinorType(), ExecutionContext.instance().allocator());
            FieldVectorUtils.allocateNew(vector, l.getValueCount());
            for (var index = 0; index < l.getValueCount(); index++) {
                var ll = l.getObject(index);
                var rr = r.getObject(index);
                if(ll instanceof Number ln && rr instanceof Number rn) {
                    if(ln instanceof BigDecimal lb && rn instanceof BigDecimal rb) {
                        FieldVectorUtils.set(vector, index, evaluate(lb, rb));
                    } else {
                        FieldVectorUtils.set(vector, index, evaluate(ln, rn, type));
                    }
                } else {
                    throw new UnsupportedOperationException("Class "+ l.getClass().getName() +" is not supported in Math expression");
                }
            }
            return vector;
        }
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
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            var ret = l.add(r);
            return ret;
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
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
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
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
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
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            if(Objects.equals(r, BigDecimal.ZERO)) return r;
            return l.divide(r, RoundingMode.HALF_EVEN);
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
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Add expression");
            };
        }

        @Override
        protected BigDecimal evaluate(BigDecimal l, BigDecimal r) {
            return l.remainder(r);
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
