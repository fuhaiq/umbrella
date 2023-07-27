package com.umbrella.calcite.adapter;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.RootAllocator;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public class SchemaFileTable extends AbstractTable implements ScannableTable {

    private final String uri;

    private final FileFormat format;

    public SchemaFileTable(String uri, FileFormat format) {
        this.uri = uri;
        this.format = format;
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root) {
        return new AbstractEnumerable<>(){
            @Override
            public Enumerator<Object[]> enumerator() {
                return new SchemaFileEnumerator<>(DataContext.Variable.CANCEL_FLAG.get(root), uri, format);
            }
        };
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        try (
                var allocator = new RootAllocator();
                var factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), format, uri);
        ) {
            var schema = factory.inspect();
            var result = schema.getFields().stream().map(field -> {
                RelDataType type;
                if(field.getType().equals(INT.getType())) {
                    type = typeFactory.createSqlType(SqlTypeName.INTEGER);
                } else if (field.getType().equals(BIGINT.getType())) {
                    type = typeFactory.createSqlType(SqlTypeName.BIGINT);
                } else if (field.getType().equals(FLOAT4.getType())) {
                    type = typeFactory.createSqlType(SqlTypeName.FLOAT);
                } else if (field.getType().equals(FLOAT8.getType())) {
                    type = typeFactory.createSqlType(SqlTypeName.DOUBLE);
                } else if (field.getType().equals(VARCHAR.getType())) {
                    type = typeFactory.createSqlType(SqlTypeName.VARCHAR);
                } else if (field.getType().getTypeID() == ArrowType.ArrowTypeID.Decimal) {
                    type = typeFactory.createSqlType(SqlTypeName.DECIMAL);
                } else throw new UnsupportedOperationException("Type "+ field.getType().toString() +" is not supported");
                return new Pair<>(field.getName(), type);
            }).toList();
            return typeFactory.createStructType(result);
        }
    }
}
