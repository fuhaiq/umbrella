package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }
    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {
        var vector = new BitVector(l.getName() + r.getName(), ExecutionContext.instance().allocator());
        vector.allocateNew(l.getValueCount());
        for (var index = 0; index < l.getValueCount(); index++) {
            var type = l.getMinorType();
            if(type == INT || type == BIGINT || type == FLOAT4 || type == FLOAT8 || type == VARCHAR || type == BIT || type == DECIMAL) {
                if(l.getObject(index) instanceof Comparable lc && r.getObject(index) instanceof Comparable rc) {
                    vector.setSafe(index, evaluate(lc, rc) ? 1 : 0);
                    continue;
                }
                throw new UnsupportedOperationException("Type "+ type +" is not supported in Bool expression");
            }
        }
        return vector;
    }
    protected abstract boolean evaluate(Comparable l, Comparable r);

    public static class And extends BooleanExpr {
        protected And(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            if(l instanceof Boolean ll && r instanceof Boolean rr) {
                return ll && rr;
            } else if (l instanceof Number ll && r instanceof Number rr) {
                return ll.intValue() == 1 && rr.intValue() == 1;
            } else {
                throw new UnsupportedOperationException("Type "+ l.getClass().getName() +" is not supported in And expression");
            }
        }
    }

    public static class Or extends BooleanExpr {
        protected Or(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            if(l instanceof Boolean ll && r instanceof Boolean rr) {
                return ll || rr;
            } else if (l instanceof Number ll && r instanceof Number rr) {
                return ll.intValue() == 1 || rr.intValue() == 1;
            } else {
                throw new UnsupportedOperationException("Type "+ l.getClass().getName() +" is not supported in Or expression");
            }
        }
    }

    public static class Eq extends BooleanExpr {
        protected Eq(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            return l.compareTo(r) == 0;
        }
    }

    public static class Neq extends BooleanExpr {
        protected Neq(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            return l.compareTo(r) != 0;
        }
    }

    public static class Gt extends BooleanExpr {
        protected Gt(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            return l.compareTo(r) > 0;
        }
    }

    public static class Ge extends BooleanExpr {
        protected Ge(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            return l.compareTo(r) >= 0;
        }
    }

    public static class Lt extends BooleanExpr {
        protected Lt(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            return l.compareTo(r) < 0;
        }
    }

    public static class Le extends BooleanExpr {
        protected Le(PhysicalExpr l, PhysicalExpr r) {
            super(l, r);
        }
        @Override
        protected boolean evaluate(Comparable l, Comparable r) {
            return l.compareTo(r) <= 0;
        }
    }
}
