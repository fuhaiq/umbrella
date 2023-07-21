package com.umbrella.logical.expr;

import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import static org.apache.arrow.vector.types.Types.MinorType.*;

public record LiteralDouble(Double n) implements LogicalExpr {
    @Override
    public Field toField(Schema schema) {
        return Field.notNullable(n.toString(), FLOAT8.getType());
    }

    @Override
    public String toString() {
        return n.toString();
    }
}
