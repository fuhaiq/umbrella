package org.umbrella.common.duckdb.reader;

import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataTypeConverter {

  public static Collection<? extends Field<?>> fields(Class<?> aClass) {
    List<Field<?>> ret = new ArrayList<>();
    ReflectionUtils.doWithFields(
            aClass,
        field -> {
          Field<?> jooqField =
              DSL.field(
                  DSL.name(field.getName()),
                  DefaultDataType.getDataType(SQLDialect.DUCKDB, field.getType()));
          ret.add(jooqField);
        },
        field ->
            !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()));
    return ret;
  }
}
