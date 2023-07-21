package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import static org.apache.arrow.vector.types.Types.MinorType.*;

public record LiteralInt(Integer n) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(n.toString(), INT.getType());
    }

    @Override
    public String toString() {
        return n.toString();
    }
}
