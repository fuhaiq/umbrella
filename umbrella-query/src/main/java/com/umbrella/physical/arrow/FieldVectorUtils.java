package com.umbrella.physical.arrow;

import com.google.common.base.Strings;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.commons.lang3.math.NumberUtils;
import org.javatuples.Pair;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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
            default -> throw new UnsupportedOperationException("Type "+ type +" is not supported.");
        };
    }

    public static FieldVector of(String name, FieldType fieldType, BufferAllocator allocator) {
        checkState(!Strings.isNullOrEmpty(name), "Name is null or empty");
        checkNotNull(fieldType, "FieldType is null");
        checkNotNull(allocator, "Allocator is null");
        var field = Field.nullable(name, fieldType.getType());
        return of(field, allocator);
    }

    public static FieldVector of(Field field, BufferAllocator allocator) {
        checkNotNull(field, "Field is null");
        checkNotNull(allocator, "Allocator is null");
        var type = field.getFieldType().getType();
        if(type.equals(INT.getType())) return new IntVector(field, allocator);
        else if (type.equals(BIGINT.getType())) return new BigIntVector(field, allocator);
        else if (type.equals(FLOAT4.getType())) return new Float4Vector(field, allocator);
        else if (type.equals(FLOAT8.getType())) return new Float8Vector(field, allocator);
        else if (type.equals(VARCHAR.getType())) return new VarCharVector(field, allocator);
        else if (type.equals(BIT.getType())) return new BitVector(field, allocator);
        else throw new UnsupportedOperationException("Type "+ type +" is not supported.");
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
        if(vector instanceof IntVector v && INT == type && value instanceof Integer i) {
            v.set(index, i);
        } else if (vector instanceof BigIntVector v && BIGINT == type && value instanceof Long i) {
            v.set(index, i);
        } else if (vector instanceof Float4Vector v && FLOAT4 == type && value instanceof Float i) {
            v.set(index, i);
        } else if (vector instanceof Float8Vector v && FLOAT8 == type && value instanceof Double i) {
            v.set(index, i);
        } else if (vector instanceof VarCharVector v && VARCHAR == type) {
            v.setSafe(index, value.toString().getBytes(StandardCharsets.UTF_8));
        } else if (vector instanceof BitVector v && BIT == type && value instanceof Boolean i) {
            v.set(index, i ? 1 : 0);
        } else throw new UnsupportedOperationException("Type " + type + " is not supported.");
    }
}
