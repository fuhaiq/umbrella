package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.DecimalVector;
import org.apache.arrow.vector.FieldVector;

import java.math.BigDecimal;

public record LiteralDecimal(BigDecimal n) implements PhysicalExpr {

    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var vector = new DecimalVector(n.toString(), ExecutionContext.instance().allocator(), n.precision(), n.scale());
        vector.allocateNew(tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            vector.setSafe(i, n);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }

    @Override
    public String toString() {
        return "DecimalExpr{" +
                "n=" + n +
                '}';
    }
}
