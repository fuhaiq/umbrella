package com.umbrella.logical.expr;


import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;


public interface LogicalExpr {

    Field toField(Schema schema);

}
