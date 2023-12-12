package org.umbrella.query.jdbc;

import org.apache.arrow.adapter.jdbc.JdbcFieldInfo;
import org.apache.arrow.adapter.jdbc.JdbcToArrowUtils;
import org.apache.arrow.vector.types.pojo.ArrowType;

import java.util.Calendar;
import java.util.function.Function;

import static org.apache.arrow.util.Preconditions.checkNotNull;

public record ExtraJDBCToArrowTypeConverter(Calendar calendar) implements Function<JdbcFieldInfo, ArrowType> {
    @Override
    public ArrowType apply(JdbcFieldInfo jdbcFieldInfo) {
        checkNotNull(jdbcFieldInfo);
        if(jdbcFieldInfo.getTypeName().equals("JSON")) {
            return new ArrowType.Struct();
        } else {
            return JdbcToArrowUtils.getArrowTypeFromJdbcType(jdbcFieldInfo, calendar);
        }
    }
}
