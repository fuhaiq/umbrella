package com.umbrella.physical.expr;

import org.apache.arrow.vector.FieldVector;

public abstract class BooleanExpr extends BinaryExpr {
    protected BooleanExpr(PhysicalExpr l, PhysicalExpr r) {
        super(l, r);
    }

    @Override
    protected FieldVector evaluate(FieldVector l, FieldVector r) {

        return null;
    }
}
