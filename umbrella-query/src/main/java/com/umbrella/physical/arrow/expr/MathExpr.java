package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.TypedFieldVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

import java.math.BigDecimal;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public abstract class MathExpr extends BinaryExpr {
    protected MathExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }

    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        var type = l.getMinorType();
        var vector = new TypedFieldVector(l.getName() + r.getName(), type);
        vector.allocateNew(l.getValueCount());
        for (var index = 0; index < l.getValueCount(); index++) {
            if(type == INT || type == BIGINT || type == FLOAT4 || type == FLOAT8 || type == DECIMAL) {
                vector.setSafe(index, evaluate(l.getObject(index), r.getObject(index), type));
                continue;
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
        return vector.getVector();
    }
    protected abstract Object evaluate(Object l, Object r, Types.MinorType type);

    public static class Add extends MathExpr {
        public Add(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected Object evaluate(Object l, Object r, Types.MinorType type) {
            if(l instanceof Integer ll && r instanceof Integer rr && type == INT) {
                return ll + rr;
            } else if (l instanceof Long ll && r instanceof Long rr && type == BIGINT) {
                return ll + rr;
            } else if (l instanceof Float ll && r instanceof Float rr && type == FLOAT4) {
                return ll + rr;
            } else if (l instanceof Double ll && r instanceof Double rr && type == FLOAT8) {
                return ll + rr;
            } else if (l instanceof BigDecimal ll && r instanceof BigDecimal rr && type == DECIMAL) {
                return ll.add(rr);
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Sub extends MathExpr {
        public Sub(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected Object evaluate(Object l, Object r, Types.MinorType type) {
            if(l instanceof Integer ll && r instanceof Integer rr && type == INT) {
                return ll - rr;
            } else if (l instanceof Long ll && r instanceof Long rr && type == BIGINT) {
                return ll - rr;
            } else if (l instanceof Float ll && r instanceof Float rr && type == FLOAT4) {
                return ll - rr;
            } else if (l instanceof Double ll && r instanceof Double rr && type == FLOAT8) {
                return ll - rr;
            } else if (l instanceof BigDecimal ll && r instanceof BigDecimal rr && type == DECIMAL) {
                return ll.subtract(rr);
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Mul extends MathExpr {
        public Mul(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected Object evaluate(Object l, Object r, Types.MinorType type) {
            if(l instanceof Integer ll && r instanceof Integer rr && type == INT) {
                return ll * rr;
            } else if (l instanceof Long ll && r instanceof Long rr && type == BIGINT) {
                return ll * rr;
            } else if (l instanceof Float ll && r instanceof Float rr && type == FLOAT4) {
                return ll * rr;
            } else if (l instanceof Double ll && r instanceof Double rr && type == FLOAT8) {
                return ll * rr;
            } else if (l instanceof BigDecimal ll && r instanceof BigDecimal rr && type == DECIMAL) {
                return ll.multiply(rr);
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Div extends MathExpr {
        public Div(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected Object evaluate(Object l, Object r, Types.MinorType type) {
            if(l instanceof Integer ll && r instanceof Integer rr && type == INT) {
                return ll / rr;
            } else if (l instanceof Long ll && r instanceof Long rr && type == BIGINT) {
                return ll / rr;
            } else if (l instanceof Float ll && r instanceof Float rr && type == FLOAT4) {
                return ll / rr;
            } else if (l instanceof Double ll && r instanceof Double rr && type == FLOAT8) {
                return ll / rr;
            } else if (l instanceof BigDecimal ll && r instanceof BigDecimal rr && type == DECIMAL) {
                return ll.divide(rr);
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Mod extends MathExpr {
        public Mod(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected Object evaluate(Object l, Object r, Types.MinorType type) {
            if(l instanceof Integer ll && r instanceof Integer rr && type == INT) {
                return ll % rr;
            } else if (l instanceof Long ll && r instanceof Long rr && type == BIGINT) {
                return ll % rr;
            } else if (l instanceof Float ll && r instanceof Float rr && type == FLOAT4) {
                return ll % rr;
            } else if (l instanceof Double ll && r instanceof Double rr && type == FLOAT8) {
                return ll % rr;
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }
}
