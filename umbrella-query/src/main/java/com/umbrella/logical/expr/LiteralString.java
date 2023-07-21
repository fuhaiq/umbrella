package com.umbrella.logical.expr;

import static org.apache.arrow.vector.types.Types.MinorType.*;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

public record LiteralString(String name) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(name, VARCHAR.getType());
    }

    @Override
    public String toString() {
        return "'"+ name +"'";
    }
}
