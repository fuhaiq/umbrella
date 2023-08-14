package com.umbrella.physical.arrow;

import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;

import java.nio.charset.StandardCharsets;

import static org.apache.arrow.vector.types.Types.MinorType.*;
import static com.google.common.base.Preconditions.*;

public class TypedFieldVector {
    private final FieldVector vector;
    private final Types.MinorType type;

    public TypedFieldVector(String name, Types.MinorType type) {
        this.type = type;
        switch (type) {
            case INT -> vector = new IntVector(name, ExecutionContext.instance().allocator());
            case BIGINT -> vector = new BigIntVector(name, ExecutionContext.instance().allocator());
            case FLOAT4 -> vector = new Float4Vector(name, ExecutionContext.instance().allocator());
            case FLOAT8 -> vector = new Float8Vector(name, ExecutionContext.instance().allocator());
            case VARCHAR -> vector = new VarCharVector(name, ExecutionContext.instance().allocator());
            case BIT -> vector = new BitVector(name, ExecutionContext.instance().allocator());
            default -> throw new UnsupportedOperationException("Type "+ type +" is not supported in FieldVectorWrapper");
        }
    }

    public void allocateNew(int valueCount) {
        if(vector instanceof BaseFixedWidthVector vv) {
            vv.allocateNew(valueCount);
        } else if (vector instanceof BaseVariableWidthVector vv) {
            vv.allocateNew(valueCount);
        } else {
            throw new UnsupportedOperationException("Vector "+ vector.getClass().getName() +" is not supported in FieldVectorWrapper");
        }
    }

    public void setSafe(int index, Object value) {
        switch (checkNotNull(vector, "vector can not be null")) {
            case IntVector v && INT == type && value instanceof Integer i -> v.setSafe(index, i);
            case BigIntVector v && BIGINT == type && value instanceof Long i -> v.setSafe(index, i);
            case Float4Vector v && FLOAT4 == type && value instanceof Float i -> v.setSafe(index, i);
            case Float8Vector v && FLOAT8 == type && value instanceof Double i -> v.setSafe(index, i);
            case VarCharVector v && VARCHAR == type && value instanceof String i ->
                    v.setSafe(index, i.getBytes(StandardCharsets.UTF_8));
            case BitVector v && BIT == type && value instanceof Boolean i -> v.setSafe(index, i ? 1 : 0);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported in FieldVectorWrapper");
        }
    }

    public FieldVector getVector() {
        return checkNotNull(vector, "vector can not be null");
    }
}
