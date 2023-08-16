package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
public abstract class BinaryExpr implements PhysicalExpr {

    protected final PhysicalExpr l;

    protected final PhysicalExpr r;

    protected BinaryExpr(PhysicalExpr l, PhysicalExpr r) {
        this.l = checkNotNull(l, "left operand is null");
        this.r = checkNotNull(r, "right operand is null");
    }

    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var ll = l.evaluate(tabular);
        var rr = r.evaluate(tabular);
        checkState(ll.getMinorType() == (rr.getMinorType()),
                "Binary expression operands do not have the same type: "
        + ll.getField().getType() + " != " + rr.getField().getType());
        checkState(ll.getValueCount() == rr.getValueCount(),
                "Binary expression operands do not have the same value count: "
        + ll.getValueCount() + " != " + rr.getValueCount());
        var vector = evaluate(ll ,rr, ll.getMinorType());
        vector.setValueCount(ll.getValueCount());
        return vector;
    }

    protected abstract FieldVector evaluate(FieldVector l, FieldVector r, Types.MinorType type);
}
