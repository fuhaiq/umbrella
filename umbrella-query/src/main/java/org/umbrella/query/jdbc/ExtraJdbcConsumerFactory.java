package org.umbrella.query.jdbc;

import org.apache.arrow.adapter.jdbc.JdbcToArrowConfig;
import org.apache.arrow.adapter.jdbc.consumer.*;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.MapVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.types.pojo.ArrowType;

import java.math.RoundingMode;
import java.util.Calendar;

public class ExtraJdbcConsumerFactory implements JdbcToArrowConfig.JdbcConsumerFactory {
    private static final int JDBC_ARRAY_VALUE_COLUMN = 2;

    @Override
    public JdbcConsumer apply(ArrowType arrowType, int columnIndex, boolean nullable, FieldVector vector, JdbcToArrowConfig config) {
        if (arrowType.getTypeID() == ArrowType.ArrowTypeID.Struct) {
            return StructConsumer.createConsumer((StructVector) vector, columnIndex, nullable);
        } else {
            return getConsumer(arrowType, columnIndex, nullable, vector, config);
        }
    }

    /**
     * 代码拷贝来自 {@link org.apache.arrow.adapter.jdbc.JdbcToArrowUtils#getConsumer}
     */
    private JdbcConsumer getConsumer(ArrowType arrowType, int columnIndex, boolean nullable,
                                     FieldVector vector, JdbcToArrowConfig config) {
        final Calendar calendar = config.getCalendar();

        switch (arrowType.getTypeID()) {
            case Bool:
                return BitConsumer.createConsumer((BitVector) vector, columnIndex, nullable);
            case Int:
                switch (((ArrowType.Int) arrowType).getBitWidth()) {
                    case 8:
                        return TinyIntConsumer.createConsumer((TinyIntVector) vector, columnIndex, nullable);
                    case 16:
                        return SmallIntConsumer.createConsumer((SmallIntVector) vector, columnIndex, nullable);
                    case 32:
                        return IntConsumer.createConsumer((IntVector) vector, columnIndex, nullable);
                    case 64:
                        return BigIntConsumer.createConsumer((BigIntVector) vector, columnIndex, nullable);
                    default:
                        return null;
                }
            case Decimal:
                final RoundingMode bigDecimalRoundingMode = config.getBigDecimalRoundingMode();
                return DecimalConsumer.createConsumer((DecimalVector) vector, columnIndex, nullable, bigDecimalRoundingMode);
            case FloatingPoint:
                switch (((ArrowType.FloatingPoint) arrowType).getPrecision()) {
                    case SINGLE:
                        return FloatConsumer.createConsumer((Float4Vector) vector, columnIndex, nullable);
                    case DOUBLE:
                        return DoubleConsumer.createConsumer((Float8Vector) vector, columnIndex, nullable);
                    default:
                        return null;
                }
            case Utf8:
            case LargeUtf8:
                return VarCharConsumer.createConsumer((VarCharVector) vector, columnIndex, nullable);
            case Binary:
            case LargeBinary:
                return BinaryConsumer.createConsumer((VarBinaryVector) vector, columnIndex, nullable);
            case Date:
                return DateConsumer.createConsumer((DateDayVector) vector, columnIndex, nullable, calendar);
            case Time:
                return TimeConsumer.createConsumer((TimeMilliVector) vector, columnIndex, nullable, calendar);
            case Timestamp:
                if (config.getCalendar() == null) {
                    return TimestampConsumer.createConsumer((TimeStampMilliVector) vector, columnIndex, nullable);
                } else {
                    return TimestampTZConsumer.createConsumer((TimeStampMilliTZVector) vector, columnIndex, nullable, calendar);
                }
            case List:
                FieldVector childVector = ((ListVector) vector).getDataVector();
                JdbcConsumer delegate = getConsumer(childVector.getField().getType(), JDBC_ARRAY_VALUE_COLUMN,
                        childVector.getField().isNullable(), childVector, config);
                return ArrayConsumer.createConsumer((ListVector) vector, delegate, columnIndex, nullable);
            case Map:
                return MapConsumer.createConsumer((MapVector) vector, columnIndex, nullable);
            case Null:
                return new NullConsumer((NullVector) vector);
            default:
                // no-op, shouldn't get here
                throw new UnsupportedOperationException("No consumer for Arrow type: " + arrowType);
        }
    }
}
