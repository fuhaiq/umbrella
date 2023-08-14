package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.TypedFieldVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

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
            if(type == INT || type == BIGINT || type == FLOAT4 || type == FLOAT8) {
                vector.setSafe(index, evaluate(l.getObject(index), r.getObject(index), type));
                continue;
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
        return vector.getVector();
    }
    protected abstract Object evaluate(Object l, Object r, Types.MinorType type);

    public static class Add extends MathExpr {
        protected Add(PhysicalExpr l, PhysicalExpr r) {
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
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Sub extends MathExpr {
        protected Sub(PhysicalExpr l, PhysicalExpr r) {
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
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Mul extends MathExpr {
        protected Mul(PhysicalExpr l, PhysicalExpr r) {
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
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Div extends MathExpr {
        protected Div(PhysicalExpr l, PhysicalExpr r) {
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
            }
            throw new UnsupportedOperationException("Type "+ type +" is not supported in Math expression");
        }
    }

    public static class Mod extends MathExpr {
        protected Mod(PhysicalExpr l, PhysicalExpr r) {
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
