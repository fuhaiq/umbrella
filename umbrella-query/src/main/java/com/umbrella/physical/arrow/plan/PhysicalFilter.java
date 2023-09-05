package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.FieldVectorUtils;
import com.umbrella.physical.arrow.VectorBatch;
import com.umbrella.physical.arrow.expr.BooleanExpr;
import com.umbrella.physical.arrow.expr.PhysicalExpr;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkState;

public class PhysicalFilter extends AbstractPhysicalPlan {
    private final PhysicalExpr expr;
    public PhysicalFilter(PhysicalPlan input, PhysicalExpr expr) {
        super(input);
        checkState((expr instanceof BooleanExpr), "Expr type "+ expr.getClass().getName() +" is not supported in PhysicalFilter");
        this.expr = expr;
    }

    @Override
    protected VectorBatch execute(VectorBatch input) {
        var retSize = 0;
        try(var bool = (BitVector) expr.evaluate(input)){
            checkState(bool.getValueCount() == input.rowCount, "数据条数不一致");
            for(var i = 0; i < bool.getValueCount(); i++) {
                retSize += bool.get(i);
            }

            var list = new ArrayList<FieldVector>();
            for(var i = 0; i < input.columnCount; i++) {
                var vector = input.getVector(i);
                checkState(bool.getValueCount() == vector.getValueCount(), "数据条数不一致");
                var ret = FieldVectorUtils.of(vector.getField(), ExecutionContext.instance().allocator());
                FieldVectorUtils.allocateNew(ret, retSize);
                int index = 0;
                for(var j = 0; j < vector.getValueCount(); j++) {
                    if(bool.get(j) == 1) {
                        FieldVectorUtils.set(ret, index, vector.getObject(j));
                        index++;
                    }
                }
                checkState(index == retSize, "数据条数不一致");
                ret.setValueCount(retSize);
                list.add(ret);
            }
            return VectorBatch.of(list);
        }
    }

    @Override
    public String toString() {
        return "PhysicalFilter{" +
                "expr=" + expr +
                '}';
    }
}
