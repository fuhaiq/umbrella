package com.umbrella.physical.arrow.expr;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.Types;

import java.math.BigDecimal;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public class LiteralExpr<T> implements PhysicalExpr {
    protected final T value;

    protected final Types.MinorType type;

    protected LiteralExpr(T value, Types.MinorType type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public FieldVector evaluate(VectorBatch tabular) {
        var vector = FieldVectorUtils.of(value.toString(), type, ExecutionContext.instance().allocator());
        FieldVectorUtils.allocateNew(vector, tabular.getRowCount());
        for (var i = 0; i < tabular.getRowCount(); i++) {
            FieldVectorUtils.set(vector, i, value);
        }
        vector.setValueCount(tabular.getRowCount());
        return vector;
    }
    public static class Bool extends LiteralExpr<Boolean> {
        public Bool(Boolean value) {
            super(value, BIT);
        }
    }

    public static class Int extends LiteralExpr<Integer> {
        public Int(Integer value) {
            super(value, INT);
        }
    }

    public static class Long extends LiteralExpr<java.lang.Long> {
        public Long(java.lang.Long value) {
            super(value, BIGINT);
        }
    }

    public static class Float extends LiteralExpr<java.lang.Float> {
        public Float(java.lang.Float value) {
            super(value, FLOAT4);
        }
    }

    public static class Double extends LiteralExpr<java.lang.Double> {
        public Double(java.lang.Double value) {
            super(value, FLOAT8);
        }
    }

    public static class Decimal extends LiteralExpr<BigDecimal> {
        public Decimal(BigDecimal value) {
            super(value, DECIMAL);
        }
    }

    public static class String extends LiteralExpr<java.lang.String> {
        public String(java.lang.String value) {
            super(value, VARCHAR);
        }
    }
}
