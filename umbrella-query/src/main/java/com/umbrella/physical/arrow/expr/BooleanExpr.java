package com.umbrella.physical.arrow.expr;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;
import org.javatuples.Pair;

import java.util.function.Function;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }
    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        var vector = new BitVector(l.getName() + r.getName(), ExecutionContext.instance().allocator());
        for (var index = 0; index < l.getValueCount(); index++) {
            vector.set(index, evaluate(l.getObject(index), r.getObject(index), l.getMinorType()) ? 1 : 0);
        }
        return vector;
    }
    protected abstract boolean evaluate(Object l, Object r, Types.MinorType type);

    public static class Eq extends BooleanExpr {
        protected Eq(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Object l, Object r, Types.MinorType type) {
            return switch (type) {
                case INT -> (int) l == (int) r;
                case BIGINT -> (long) l == (long) r;
                case FLOAT4 -> (float) l == (float) r;
                case FLOAT8 -> (double) l == (double) r;
                case VARCHAR -> l.toString().compareTo(r.toString()) == 0;
                case BIT -> (boolean) l == (boolean) r;
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Binary expression");
            };
        }
    }

    public static class Neq extends BooleanExpr {
        protected Neq(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Object l, Object r, Types.MinorType type) {
            return switch (type) {
                case INT -> (int) l != (int) r;
                case BIGINT -> (long) l != (long) r;
                case FLOAT4 -> (float) l != (float) r;
                case FLOAT8 -> (double) l != (double) r;
                case VARCHAR -> l.toString().compareTo(r.toString()) != 0;
                case BIT -> (boolean) l != (boolean) r;
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Binary expression");
            };
        }
    }

    public static class Gt extends BooleanExpr {
        protected Gt(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Object l, Object r, Types.MinorType type) {
            return switch (type) {
                case INT -> (int) l > (int) r;
                case BIGINT -> (long) l > (long) r;
                case FLOAT4 -> (float) l > (float) r;
                case FLOAT8 -> (double) l > (double) r;
                case VARCHAR -> l.toString().compareTo(r.toString()) > 0;
                case BIT -> (boolean) l != (boolean) r;
                default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in Binary expression");
            };
        }
    }
}
