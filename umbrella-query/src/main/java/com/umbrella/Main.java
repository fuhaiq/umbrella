package com.umbrella;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.dataset.source.DatasetFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.ExpressionType;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.umbrella.logical.fn.ExprFunction.*;

public class Main {
    public static void main(String[] args) {
//        var ctx = ExecutionContext.instance();
//        var df = ctx.parquet("file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet")
//                .select(as(add(lit(123), lit(123)),"lll"), col("s_name"))
//                .filter(eq(col("s_name"), lit("Mike")))
//                .orderBy(asc(col("s_name")))
//                .limit(100);
//        System.out.println(df.schema().toJson());
//        df.explain();
//        ctx.stop();
    }
}
