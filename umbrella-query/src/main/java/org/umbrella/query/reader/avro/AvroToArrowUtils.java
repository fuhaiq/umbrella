package org.umbrella.query.reader.avro;

import static org.apache.arrow.vector.types.FloatingPointPrecision.DOUBLE;
import static org.apache.arrow.vector.types.FloatingPointPrecision.SINGLE;

import java.util.*;
import org.apache.arrow.AvroToArrowConfig;
import org.apache.arrow.util.Preconditions;
import org.apache.arrow.vector.dictionary.DictionaryEncoder;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.types.DateUnit;
import org.apache.arrow.vector.types.TimeUnit;
import org.apache.arrow.vector.types.Types;
import org.apache.arrow.vector.types.UnionMode;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.DictionaryEncoding;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.util.JsonStringArrayList;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;

/**
 * 完全拷贝代码来自 {@link org.apache.arrow.AvroToArrowUtils}
 */
public class AvroToArrowUtils {

    private static ArrowType createDecimalArrowType(LogicalTypes.Decimal logicalType) {
        final int scale = logicalType.getScale();
        final int precision = logicalType.getPrecision();
        Preconditions.checkArgument(precision > 0 && precision <= 38,
                "Precision must be in range of 1 to 38");
        Preconditions.checkArgument(scale >= 0 && scale <= 38,
                "Scale must be in range of 0 to 38.");
        Preconditions.checkArgument(scale <= precision,
                "Invalid decimal scale: %s (greater than precision: %s)", scale, precision);

        return new ArrowType.Decimal(precision, scale, 128);
    }

    private static String getDefaultFieldName(ArrowType type) {
        Types.MinorType minorType = Types.getMinorTypeForArrowType(type);
        return minorType.name().toLowerCase();
    }

    public static Field avroSchemaToField(Schema schema, String name, AvroToArrowConfig config) {
        return avroSchemaToField(schema, name, config, null);
    }

    private static Field avroSchemaToField(
            Schema schema,
            String name,
            AvroToArrowConfig config,
            Map<String, String> externalProps) {

        final Type type = schema.getType();
        final LogicalType logicalType = schema.getLogicalType();
        final List<Field> children = new ArrayList<>();
        final FieldType fieldType;

        switch (type) {
            case UNION:
                for (int i = 0; i < schema.getTypes().size(); i++) {
                    Schema childSchema = schema.getTypes().get(i);
                    // Union child vector should use default name
                    children.add(avroSchemaToField(childSchema, null, config));
                }
                fieldType = createFieldType(new ArrowType.Union(UnionMode.Sparse, null), schema, externalProps);
                break;
            case ARRAY:
                Schema elementSchema = schema.getElementType();
                children.add(avroSchemaToField(elementSchema, elementSchema.getName(), config));
                fieldType = createFieldType(new ArrowType.List(), schema, externalProps);
                break;
            case MAP:
                // MapVector internal struct field and key field should be non-nullable
                FieldType keyFieldType = new FieldType(/*nullable=*/false, new ArrowType.Utf8(), /*dictionary=*/null);
                Field keyField = new Field("key", keyFieldType, /*children=*/null);
                Field valueField = avroSchemaToField(schema.getValueType(), "value", config);

                FieldType structFieldType = new FieldType(false, new ArrowType.Struct(), /*dictionary=*/null);
                Field structField = new Field("internal", structFieldType, Arrays.asList(keyField, valueField));
                children.add(structField);
                fieldType = createFieldType(new ArrowType.Map(/*keySorted=*/false), schema, externalProps);
                break;
            case RECORD:
                final Set<String> skipFieldNames = config.getSkipFieldNames();
                for (int i = 0; i < schema.getFields().size(); i++) {
                    final Schema.Field field = schema.getFields().get(i);
                    Schema childSchema = field.schema();
                    String fullChildName = String.format("%s.%s", name, field.name());
                    if (!skipFieldNames.contains(fullChildName)) {
                        final Map<String, String> extProps = new HashMap<>();
                        String doc = field.doc();
                        Set<String> aliases = field.aliases();
                        if (doc != null) {
                            extProps.put("doc", doc);
                        }
                        if (aliases != null) {
                            extProps.put("aliases", convertAliases(aliases));
                        }
                        children.add(avroSchemaToField(childSchema, fullChildName, config, extProps));
                    }
                }
                fieldType = createFieldType(new ArrowType.Struct(), schema, externalProps);
                break;
            case ENUM:
                DictionaryProvider.MapDictionaryProvider provider = config.getProvider();
                int current = provider.getDictionaryIds().size();
                int enumCount = schema.getEnumSymbols().size();
                ArrowType.Int indexType = DictionaryEncoder.getIndexType(enumCount);

                fieldType = createFieldType(indexType, schema, externalProps,
                        new DictionaryEncoding(current, /*ordered=*/false, /*indexType=*/indexType));
                break;

            case STRING:
                fieldType = createFieldType(new ArrowType.Utf8(), schema, externalProps);
                break;
            case FIXED:
                final ArrowType fixedArrowType;
                if (logicalType instanceof LogicalTypes.Decimal) {
                    fixedArrowType = createDecimalArrowType((LogicalTypes.Decimal) logicalType);
                } else {
                    fixedArrowType = new ArrowType.FixedSizeBinary(schema.getFixedSize());
                }
                fieldType = createFieldType(fixedArrowType, schema, externalProps);
                break;
            case INT:
                final ArrowType intArrowType;
                if (logicalType instanceof LogicalTypes.Date) {
                    intArrowType = new ArrowType.Date(DateUnit.DAY);
                } else if (logicalType instanceof LogicalTypes.TimeMillis) {
                    intArrowType = new ArrowType.Time(TimeUnit.MILLISECOND, 32);
                } else {
                    intArrowType = new ArrowType.Int(32, /*signed=*/true);
                }
                fieldType = createFieldType(intArrowType, schema, externalProps);
                break;
            case BOOLEAN:
                fieldType = createFieldType(new ArrowType.Bool(), schema, externalProps);
                break;
            case LONG:
                final ArrowType longArrowType;
                if (logicalType instanceof LogicalTypes.TimeMicros) {
                    longArrowType = new ArrowType.Time(TimeUnit.MICROSECOND, 64);
                } else if (logicalType instanceof LogicalTypes.TimestampMillis) {
                    longArrowType = new ArrowType.Timestamp(TimeUnit.MILLISECOND, null);
                } else if (logicalType instanceof LogicalTypes.TimestampMicros) {
                    longArrowType = new ArrowType.Timestamp(TimeUnit.MICROSECOND, null);
                } else {
                    longArrowType = new ArrowType.Int(64, /*signed=*/true);
                }
                fieldType = createFieldType(longArrowType, schema, externalProps);
                break;
            case FLOAT:
                fieldType = createFieldType(new ArrowType.FloatingPoint(SINGLE), schema, externalProps);
                break;
            case DOUBLE:
                fieldType = createFieldType(new ArrowType.FloatingPoint(DOUBLE), schema, externalProps);
                break;
            case BYTES:
                final ArrowType bytesArrowType;
                if (logicalType instanceof LogicalTypes.Decimal) {
                    bytesArrowType = createDecimalArrowType((LogicalTypes.Decimal) logicalType);
                } else {
                    bytesArrowType = new ArrowType.Binary();
                }
                fieldType = createFieldType(bytesArrowType, schema, externalProps);
                break;
            case NULL:
                fieldType = createFieldType(ArrowType.Null.INSTANCE, schema, externalProps);
                break;
            default:
                // no-op, shouldn't get here
                throw new UnsupportedOperationException();
        }

        if (name == null) {
            name = getDefaultFieldName(fieldType.getType());
        }
        return new Field(name, fieldType, children.size() == 0 ? null : children);
    }

    private static Map<String, String> getMetaData(Schema schema) {
        Map<String, String> metadata = new HashMap<>();
        schema.getObjectProps().forEach((k, v) -> metadata.put(k, v.toString()));
        return metadata;
    }

    private static Map<String, String> getMetaData(Schema schema, Map<String, String> externalProps) {
        Map<String, String> metadata = getMetaData(schema);
        if (externalProps != null) {
            metadata.putAll(externalProps);
        }
        return metadata;
    }

    private static FieldType createFieldType(ArrowType arrowType, Schema schema, Map<String, String> externalProps) {
        return createFieldType(arrowType, schema, externalProps, /*dictionary=*/null);
    }

    private static FieldType createFieldType(
            ArrowType arrowType,
            Schema schema,
            Map<String, String> externalProps,
            DictionaryEncoding dictionary) {

        return new FieldType(/*nullable=*/false, arrowType, dictionary,
                getMetaData(schema, externalProps));
    }

    private static String convertAliases(Set<String> aliases) {
        JsonStringArrayList jsonList = new JsonStringArrayList();
        aliases.stream().forEach(a -> jsonList.add(a));
        return jsonList.toString();
    }
}
