package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }

    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        var vector = new BitVector(l.getName() + r.getName(), ExecutionContext.instance().allocator());
        switch (l) {
            case IntVector i -> {
                var ir = (IntVector) r;
                for (var index = 0; index < i.getValueCount(); index++) {
                    vector.set(index, evaluate(i.get(index), ir.get(index), l.getMinorType()) ? 1 : 0);
                }
            }
            case BigIntVector bi -> {
                var bir = (BigIntVector) r;
                for (var index = 0; index < bi.getValueCount(); index++) {
                    vector.set(index, evaluate(bi.get(index), bir.get(index), l.getMinorType()) ? 1 : 0);
                }
            }
            case Float4Vector f4 -> {
                var f4r = (Float4Vector) r;
                for (var index = 0; index < f4.getValueCount(); index++) {
                    vector.set(index, evaluate(f4.get(index), f4r.get(index), l.getMinorType()) ? 1 : 0);
                }
            }
            case Float8Vector f8 -> {
                var f8r = (Float4Vector) r;
                for (var index = 0; index < f8.getValueCount(); index++) {
                    vector.set(index, evaluate(f8.get(index), f8r.get(index), l.getMinorType()) ? 1 : 0);
                }
            }
            case VarCharVector chi -> {
                var chir = (VarCharVector) r;
                for (var index = 0; index < chi.getValueCount(); index++) {
                    vector.set(index, evaluate(chi.get(index), chir.get(index), l.getMinorType()) ? 1 : 0);
                }
            }
            default -> throw new UnsupportedOperationException("Type "+ l.getMinorType() +" is not supported in Binary expression");
        }
        return vector;
    }

    protected abstract boolean evaluate(Object l, Object r, Types.MinorType type);

    public static class And extends BooleanExpr {
        protected And(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Object l, Object r, Types.MinorType type) {
            return switch (type) {
                case INT -> (int) l == (int) r;
                case BIGINT -> (long) l == (long) r;
                case FLOAT4 -> (float) l == (float) r;
                case FLOAT8 -> (double) l == (double) r;
                case VARCHAR -> l.equals(r);
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Binary expression");
            };
        }
    }
}
