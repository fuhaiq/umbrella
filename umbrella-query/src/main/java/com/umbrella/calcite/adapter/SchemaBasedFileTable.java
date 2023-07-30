package com.umbrella.calcite.adapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.memory.RootAllocator;

import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.rules.ProjectTableScanRule;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.apache.arrow.vector.types.Types.MinorType.*;

public class SchemaBasedFileTable extends AbstractTable implements ScannableTable, ProjectableFilterableTable {

    private final String uri;

    private final FileFormat format;

    private RelDataType relDataType;

    public SchemaBasedFileTable(String uri, FileFormat format) {
        this.uri = uri;
        this.format = format;
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root) {
        return new AbstractEnumerable<>(){
            @Override
            public Enumerator<Object[]> enumerator() {
                return new SchemaBasedFileEnumerator<>(DataContext.Variable.CANCEL_FLAG.get(root), uri, format,
                        new ScanOptions(/*batchSize*/ 32768),
                List.of());
            }
        };
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root, List<RexNode> f, int @Nullable [] projects) {
        var names = new String[projects.length];
        for (var i = 0; i < projects.length; i++) {
            names[i] = relDataType.getFieldNames().get(projects[i]);
        }
        List<RexCall> filters = (f == null || f.size() ==0) ? List.of() :
                f.stream().map(it -> (RexCall) it).toList();
        return new AbstractEnumerable<>(){
            @Override
            public Enumerator<Object[]> enumerator() {
                return new SchemaBasedFileEnumerator<>(DataContext.Variable.CANCEL_FLAG.get(root), uri, format,
                        new ScanOptions(/*batchSize*/ 32768, Optional.of(names)),
                        filters);
            }
        };
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        if(relDataType != null) return relDataType;
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
                } else if (field.getType().getTypeID() == ArrowType.ArrowTypeID.Timestamp) {
                    type = typeFactory.createSqlType(SqlTypeName.TIMESTAMP);
                } else throw new UnsupportedOperationException("Type "+ field.getType().toString() +" is not supported");
                return new Pair<>(field.getName(), type);
            }).toList();
            relDataType = typeFactory.createStructType(result);
            return relDataType;
        }
    }
}
