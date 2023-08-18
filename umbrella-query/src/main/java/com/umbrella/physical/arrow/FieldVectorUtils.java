package com.umbrella.physical.arrow;

import com.google.common.base.Strings;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.Field;

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
            case null, default -> throw new UnsupportedOperationException("Type "+ type +" is not supported.");
        };
    }

    public static DecimalVector of(String name, BigDecimal value, BufferAllocator allocator) {
        return new DecimalVector(name, allocator, value.precision(), value.scale());
    }

    public static FieldVector of(Field field, Types.MinorType type, BufferAllocator allocator) {
        checkNotNull(field, "Field is null");
        checkNotNull(type, "Type is null");
        checkNotNull(allocator, "Allocator is null");
        return switch (type) {
            case INT ->  new IntVector(field, allocator);
            case BIGINT -> new BigIntVector(field, allocator);
            case FLOAT4 -> new Float4Vector(field, allocator);
            case FLOAT8 -> new Float8Vector(field, allocator);
            case VARCHAR -> new VarCharVector(field, allocator);
            case BIT -> new BitVector(field, allocator);
            case DECIMAL -> new DecimalVector(field, allocator);
            case null, default -> throw new UnsupportedOperationException("Type "+ type +" is not supported.");
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
            case IntVector v when INT == type && value instanceof Integer i -> v.set(index, i);
            case BigIntVector v when BIGINT == type && value instanceof Long i -> v.set(index, i);
            case Float4Vector v when FLOAT4 == type && value instanceof Float i -> v.set(index, i);
            case Float8Vector v when FLOAT8 == type && value instanceof Double i -> v.set(index, i);
            case VarCharVector v when VARCHAR == type && value instanceof String i ->
                    v.set(index, i.getBytes(StandardCharsets.UTF_8));
            case BitVector v when BIT == type && value instanceof Boolean i -> v.set(index, i ? 1 : 0);
            case DecimalVector v when DECIMAL == type && value instanceof BigDecimal i -> v.set(index, i);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported.");
        }
    }

    public static void castAndSet(FieldVector vector, int index, Object value) {
        checkNotNull(vector, "vector is null");
        checkNotNull(value, "value is null");
        var type = vector.getMinorType();
        switch (vector) {
            case IntVector v when INT == type -> v.set(index, (int) value);
            case BigIntVector v when BIGINT == type -> v.set(index, (long) value);
            case Float4Vector v when FLOAT4 == type -> v.set(index, (float) value);
            case Float8Vector v when FLOAT8 == type -> v.set(index, (double) value);
            case VarCharVector v when VARCHAR == type ->
                    v.set(index, ((String) value).getBytes(StandardCharsets.UTF_8));
            case BitVector v when BIT == type -> v.set(index, (boolean) value ? 1 : 0);
            case null, default ->
                    throw new UnsupportedOperationException("Type " + type + " is not supported.");
        }
    }
}
