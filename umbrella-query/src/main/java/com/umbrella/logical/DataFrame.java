package com.umbrella.logical;

import com.umbrella.logical.expr.AggExpr;
import com.umbrella.logical.expr.LogicalExpr;
import org.apache.arrow.vector.types.pojo.Schema;


import java.util.List;

public interface DataFrame {

    DataFrame select(LogicalExpr... expr);

    DataFrame filter(LogicalExpr expr);

    DataFrame where(LogicalExpr expr);

    DataFrame agg(List<LogicalExpr> groupBy, List<AggExpr> aggExpr);

    Schema schema();

    void explain();

}
