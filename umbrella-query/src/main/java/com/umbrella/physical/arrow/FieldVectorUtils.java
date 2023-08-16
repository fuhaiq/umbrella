package com.umbrella.physical.arrow;

import com.google.common.base.Strings;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.FieldType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.arrow.vector.types.Types.MinorType.*;

public final class FieldVectorUtils {

    public static FieldVector of(String name, Types.MinorType type, BufferAllocator allocator) {
        checkState(!Strings.isNullOrEmpty(name), "name is null or empty");
        checkNotNull(type, "Type is null");
        checkNotNull(allocator, "Allocator is null");
        return switch (type) {
            case INT ->  new IntVector(name, allocator);
            case BIGINT -> new BigIntVector(name, allocator);
            case FLOAT4 -> new Float4Vector(name, allocator);
            case FLOAT8 -> new Float8Vector(name, allocator);
            case VARCHAR -> new VarCharVector(name, allocator);
            case BIT -> new BitVector(name, allocator);
            case DECIMAL -> new DecimalVector(name, FieldType.nullable(type.getType()), allocator);
            default -> throw new UnsupportedOperationException("Type "+ type +" is not supported.");
        };
    }

    public static void allocateNew(FieldVector vector, int valueCount) {
        if(vector instanceof BaseFixedWidthVector vv) {
            vv.allocateNew(valueCount);
        } else if (vector instanceof BaseVariableWidthVector vv) {
            vv.allocateNew(valueCount);
        } else {
            throw new UnsupportedOperationException("Vector "+ vector.getClass().getName() +" is not supported.");
        }
    }

    public static void set(FieldVector vector, int index, Object value) {
        checkNotNull(vector, "vector is null");
        checkNotNull(value, "value is null");
        var type = vector.getMinorType();
        switch (vector) {
            case IntVector v && INT == type && value instanceof Integer i -> v.set(index, i);
            case BigIntVector v && BIGINT == type && value instanceof Long i -> v.set(index, i);
            case Float4Vector v && FLOAT4 == type && value instanceof Float i -> v.set(index, i);
            case Float8Vector v && FLOAT8 == type && value instanceof Double i -> v.set(index, i);
            case VarCharVector v && VARCHAR == type && value instanceof String i ->
                    v.set(index, i.getBytes(StandardCharsets.UTF_8));
            case BitVector v && BIT == type && value instanceof Boolean i -> v.set(index, i ? 1 : 0);
            case DecimalVector v && DECIMAL == type && value instanceof BigDecimal i -> v.set(index, i);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported.");
        }
    }

    public static void castAndSet(FieldVector vector, int index, Object value) {
        checkNotNull(vector, "vector is null");
        checkNotNull(value, "value is null");
        var type = vector.getMinorType();
        switch (vector) {
            case IntVector v && INT == type -> v.set(index, (int) value);
            case BigIntVector v && BIGINT == type -> v.set(index, (long) value);
            case Float4Vector v && FLOAT4 == type -> v.set(index, (float) value);
            case Float8Vector v && FLOAT8 == type -> v.set(index, (double) value);
            case VarCharVector v && VARCHAR == type ->
                    v.set(index, ((String) value).getBytes(StandardCharsets.UTF_8));
            case BitVector v && BIT == type -> v.set(index, (boolean) value ? 1 : 0);
            case DecimalVector v && DECIMAL == type -> v.set(index, (BigDecimal) value);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported.");
        }
    }

    public static void setSafe(FieldVector vector, int index, Object value) {
        checkNotNull(vector, "vector is null");
        checkNotNull(value, "value is null");
        var type = vector.getMinorType();
        switch (vector) {
            case IntVector v && INT == type && value instanceof Integer i -> v.setSafe(index, i);
            case BigIntVector v && BIGINT == type && value instanceof Long i -> v.setSafe(index, i);
            case Float4Vector v && FLOAT4 == type && value instanceof Float i -> v.setSafe(index, i);
            case Float8Vector v && FLOAT8 == type && value instanceof Double i -> v.setSafe(index, i);
            case VarCharVector v && VARCHAR == type && value instanceof String i ->
                    v.setSafe(index, i.getBytes(StandardCharsets.UTF_8));
            case BitVector v && BIT == type && value instanceof Boolean i -> v.setSafe(index, i ? 1 : 0);
            case DecimalVector v && DECIMAL == type && value instanceof BigDecimal i -> v.setSafe(index, i);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported.");
        }
    }

    public static void castAndSetSafe(FieldVector vector, int index, Object value) {
        checkNotNull(vector, "vector is null");
        checkNotNull(value, "value is null");
        var type = vector.getMinorType();
        switch (vector) {
            case IntVector v && INT == type -> v.setSafe(index, (int) value);
            case BigIntVector v && BIGINT == type -> v.setSafe(index, (long) value);
            case Float4Vector v && FLOAT4 == type -> v.setSafe(index, (float) value);
            case Float8Vector v && FLOAT8 == type -> v.setSafe(index, (double) value);
            case VarCharVector v && VARCHAR == type ->
                    v.setSafe(index, ((String) value).getBytes(StandardCharsets.UTF_8));
            case BitVector v && BIT == type -> v.setSafe(index, (boolean) value ? 1 : 0);
            case DecimalVector v && DECIMAL == type -> v.setSafe(index, (BigDecimal) value);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported.");
        }
    }
}
