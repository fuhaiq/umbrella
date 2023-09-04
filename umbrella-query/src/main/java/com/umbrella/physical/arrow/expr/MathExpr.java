package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public abstract class MathExpr extends BinaryExpr {
    protected MathExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }
    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        Types.MinorType type = l.getMinorType().compareTo(r.getMinorType()) > 0 ? l.getMinorType() : r.getMinorType();
        FieldVector vector = FieldVectorUtils.of(l.getName() + r.getName(), type, ExecutionContext.instance().allocator());
        FieldVectorUtils.allocateNew(vector, l.getValueCount());
        for (var index = 0; index < l.getValueCount(); index++) {
            Number lValue = (Number) l.getObject(index);
            Number rValue = (Number) r.getObject(index);
            FieldVectorUtils.set(vector, index, evaluate(lValue, rValue, l.getMinorType(), r.getMinorType()));
        }
        return vector;
    }
    protected abstract Number evaluate(Number l, Number r, Types.MinorType lType, Types.MinorType rType);

    public static class Add extends MathExpr {
        public Add(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }

        @Override
        protected Number evaluate(Number l, Number r, Types.MinorType lType, Types.MinorType rType) {
            if(lType == INT) {
                var lValue = l.intValue();
                if(rType == INT) return lValue + r.intValue();
                if(rType == BIGINT) return lValue + r.longValue();
                if(rType == FLOAT4) return lValue + r.floatValue();
                if(rType == FLOAT8) return lValue + r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == BIGINT) {
                var lValue = l.longValue();
                if(rType == INT) return lValue + r.intValue();
                if(rType == BIGINT) return lValue + r.longValue();
                if(rType == FLOAT4) return lValue + r.floatValue();
                if(rType == FLOAT8) return lValue + r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT4) {
                var lValue = l.floatValue();
                if(rType == INT) return lValue + r.intValue();
                if(rType == BIGINT) return lValue + r.longValue();
                if(rType == FLOAT4) return lValue + r.floatValue();
                if(rType == FLOAT8) return lValue + r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT8) {
                var lValue = l.doubleValue();
                if(rType == INT) return lValue + r.intValue();
                if(rType == BIGINT) return lValue + r.longValue();
                if(rType == FLOAT4) return lValue + r.floatValue();
                if(rType == FLOAT8) return lValue + r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else throw new UnsupportedOperationException("Type "+ lType +" is not supported in Add expression");

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
        protected Number evaluate(Number l, Number r, Types.MinorType lType, Types.MinorType rType) {
            if(lType == INT) {
                var lValue = l.intValue();
                if(rType == INT) return lValue - r.intValue();
                if(rType == BIGINT) return lValue - r.longValue();
                if(rType == FLOAT4) return lValue - r.floatValue();
                if(rType == FLOAT8) return lValue - r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == BIGINT) {
                var lValue = l.longValue();
                if(rType == INT) return lValue - r.intValue();
                if(rType == BIGINT) return lValue - r.longValue();
                if(rType == FLOAT4) return lValue - r.floatValue();
                if(rType == FLOAT8) return lValue - r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT4) {
                var lValue = l.floatValue();
                if(rType == INT) return lValue - r.intValue();
                if(rType == BIGINT) return lValue - r.longValue();
                if(rType == FLOAT4) return lValue - r.floatValue();
                if(rType == FLOAT8) return lValue - r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT8) {
                var lValue = l.doubleValue();
                if(rType == INT) return lValue - r.intValue();
                if(rType == BIGINT) return lValue - r.longValue();
                if(rType == FLOAT4) return lValue - r.floatValue();
                if(rType == FLOAT8) return lValue - r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else throw new UnsupportedOperationException("Type "+ lType +" is not supported in Add expression");
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
        protected Number evaluate(Number l, Number r, Types.MinorType lType, Types.MinorType rType) {
            if(lType == INT) {
                var lValue = l.intValue();
                if(rType == INT) return lValue * r.intValue();
                if(rType == BIGINT) return lValue * r.longValue();
                if(rType == FLOAT4) return lValue * r.floatValue();
                if(rType == FLOAT8) return lValue * r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == BIGINT) {
                var lValue = l.longValue();
                if(rType == INT) return lValue * r.intValue();
                if(rType == BIGINT) return lValue * r.longValue();
                if(rType == FLOAT4) return lValue * r.floatValue();
                if(rType == FLOAT8) return lValue * r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT4) {
                var lValue = l.floatValue();
                if(rType == INT) return lValue * r.intValue();
                if(rType == BIGINT) return lValue * r.longValue();
                if(rType == FLOAT4) return lValue * r.floatValue();
                if(rType == FLOAT8) return lValue * r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT8) {
                var lValue = l.doubleValue();
                if(rType == INT) return lValue * r.intValue();
                if(rType == BIGINT) return lValue * r.longValue();
                if(rType == FLOAT4) return lValue * r.floatValue();
                if(rType == FLOAT8) return lValue * r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else throw new UnsupportedOperationException("Type "+ lType +" is not supported in Add expression");
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
        protected Number evaluate(Number l, Number r, Types.MinorType lType, Types.MinorType rType) {
            if(lType == INT) {
                var lValue = l.intValue();
                if(rType == INT) return lValue / r.intValue();
                if(rType == BIGINT) return lValue / r.longValue();
                if(rType == FLOAT4) return lValue / r.floatValue();
                if(rType == FLOAT8) return lValue / r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == BIGINT) {
                var lValue = l.longValue();
                if(rType == INT) return lValue / r.intValue();
                if(rType == BIGINT) return lValue / r.longValue();
                if(rType == FLOAT4) return lValue / r.floatValue();
                if(rType == FLOAT8) return lValue / r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT4) {
                var lValue = l.floatValue();
                if(rType == INT) return lValue / r.intValue();
                if(rType == BIGINT) return lValue / r.longValue();
                if(rType == FLOAT4) return lValue / r.floatValue();
                if(rType == FLOAT8) return lValue / r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT8) {
                var lValue = l.doubleValue();
                if(rType == INT) return lValue / r.intValue();
                if(rType == BIGINT) return lValue / r.longValue();
                if(rType == FLOAT4) return lValue / r.floatValue();
                if(rType == FLOAT8) return lValue / r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else throw new UnsupportedOperationException("Type "+ lType +" is not supported in Add expression");
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
        protected Number evaluate(Number l, Number r, Types.MinorType lType, Types.MinorType rType) {
            if(lType == INT) {
                var lValue = l.intValue();
                if(rType == INT) return lValue % r.intValue();
                if(rType == BIGINT) return lValue % r.longValue();
                if(rType == FLOAT4) return lValue % r.floatValue();
                if(rType == FLOAT8) return lValue % r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == BIGINT) {
                var lValue = l.longValue();
                if(rType == INT) return lValue % r.intValue();
                if(rType == BIGINT) return lValue % r.longValue();
                if(rType == FLOAT4) return lValue % r.floatValue();
                if(rType == FLOAT8) return lValue % r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT4) {
                var lValue = l.floatValue();
                if(rType == INT) return lValue % r.intValue();
                if(rType == BIGINT) return lValue % r.longValue();
                if(rType == FLOAT4) return lValue % r.floatValue();
                if(rType == FLOAT8) return lValue % r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else if (lType == FLOAT8) {
                var lValue = l.doubleValue();
                if(rType == INT) return lValue % r.intValue();
                if(rType == BIGINT) return lValue % r.longValue();
                if(rType == FLOAT4) return lValue % r.floatValue();
                if(rType == FLOAT8) return lValue % r.doubleValue();
                else throw new UnsupportedOperationException("Type "+ rType +" is not supported in Add expression");
            } else throw new UnsupportedOperationException("Type "+ lType +" is not supported in Add expression");
        }
    }
}
