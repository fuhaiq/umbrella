package org.umbrella.query.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.adapter.jdbc.consumer.BaseConsumer;
import org.apache.arrow.adapter.jdbc.consumer.JdbcConsumer;
import org.apache.arrow.vector.complex.StructVector;

import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class StructConsumer {

    public static JdbcConsumer<StructVector> createConsumer(StructVector vector, int index, boolean nullable) {
        if (nullable) {
            return new StructConsumer.NullableStructConsumer(vector, index);
        } else {
            return new StructConsumer.NonNullableStructConsumer(vector, index);
        }
    }

    static class NullableStructConsumer extends BaseConsumer<StructVector> {

        public NullableStructConsumer(StructVector vector, int index) {
            super(vector, index);
        }


        /**
         * 一些参考
         * <li>{@link org.apache.arrow.vector.complex.impl.NullableStructWriter}
         * <li>{@link org.apache.arrow.vector.complex.impl.SingleStructWriter}
         * <li>{@link org.apache.arrow.vector.complex.impl.StructOrListWriterImpl}
         * <li>{@link org.apache.arrow.consumers.AvroStructConsumer}
         * <li>https://www.vertica.com/docs/11.0.x/HTML/Content/Authoring/ConnectingToVertica/ClientJDBC/ComplexTypesInJDBC.htm
         */
        @Override
        public void consume(ResultSet resultSet) throws SQLException {
            var obj = resultSet.getObject(columnIndexInResultSet);
            if(obj instanceof java.sql.Struct struct) {
                if (!resultSet.wasNull()) {
                    //TODO
                }
                currentIndex++;
            } else {
                if(log.isDebugEnabled()) log.debug(obj.toString());
                throw new SQLException("JDBC 中的数据不是结构体,实际为:" + obj.getClass().getName());
            }

        }
    }

    static class NonNullableStructConsumer extends BaseConsumer<StructVector> {

        public NonNullableStructConsumer(StructVector vector, int index) {
            super(vector, index);
        }

        @Override
        public void consume(ResultSet resultSet) throws SQLException {
            var obj = resultSet.getObject(columnIndexInResultSet);
            if(obj instanceof java.sql.Struct struct) {
                //TODO
                currentIndex++;
            } else {
                if(log.isDebugEnabled()) log.debug(obj.toString());
                throw new SQLException("JDBC 中的数据不是结构体,实际为:" + obj.getClass().getName());
            }
        }
    }
}
