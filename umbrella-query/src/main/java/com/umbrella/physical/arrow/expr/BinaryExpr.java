package com.umbrella.physical.arrow.expr;

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;

import static com.google.common.base.Preconditions.*;
public abstract class BinaryExpr implements PhysicalExpr {

    protected final PhysicalExpr l;

    protected final PhysicalExpr r;

    protected BinaryExpr(PhysicalExpr l, PhysicalExpr r) {
        this.l = l;
        this.r = r;
    }

    @Override
    public FieldVector evaluate(VectorSchemaRoot tabular) {
        var ll = l.evaluate(tabular);
        var rr = r.evaluate(tabular);
        checkState(ll.getField().getType().equals(rr.getField().getType()),
                "Binary expression operands do not have the same type: "
        + ll.getField().getType() + " != " + rr.getField().getType());
        checkState(ll.getValueCount() == rr.getValueCount(),
                "Binary expression operands do not have the same value count: "
        + ll.getValueCount() + " != " + rr.getValueCount());
        var vector = evaluate(ll ,rr);
        vector.setValueCount(ll.getValueCount());
        return vector;
    }

    protected abstract FieldVector evaluate(FieldVector l, FieldVector r);
}
