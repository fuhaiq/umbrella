package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import com.umbrella.physical.arrow.VectorBatch;
import org.apache.arrow.vector.DecimalVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.types.Types;

import java.util.ArrayList;
import java.util.List;

import static org.apache.arrow.vector.types.Types.MinorType.FLOAT4;

public class PhysicalDecimal2Float extends AbstractPhysicalPlan {
    private final List<String> columns;
    public PhysicalDecimal2Float(PhysicalPlan input, List<String> columns) {
        super(input);
        this.columns = columns;
    }

    @Override
    protected VectorBatch execute(VectorBatch input) {
        var res = new ArrayList<FieldVector>(input.columnCount);
        for (var i = 0; i < input.columnCount; i++) {
            var vector = input.getVector(i);
            FieldVector target;
            if(columns.contains(vector.getName()) && vector instanceof DecimalVector dv) {
                target = new Float4Vector(vector.getName(), ExecutionContext.instance().allocator());
                FieldVectorUtils.allocateNew(target, input.rowCount);
                for (var j = 0; j < input.rowCount; j++) {
                    FieldVectorUtils.set(target, j, dv.getObject(j).floatValue());
                }
                target.setValueCount(input.rowCount);
            } else {
                target = FieldVectorUtils.of(vector.getField(), vector.getMinorType(), ExecutionContext.instance().allocator());
                vector.makeTransferPair(target).transfer();
            }
            res.add(target);
        }
        return VectorBatch.of(res);
    }

    @Override
    public String toString() {
        return "PhysicalDecimal2Float{" +
                "columns=" + columns +
                '}';
    }
}
