package com.umbrella;

import com.umbrella.execution.ExecutionContext;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.calcite.linq4j.tree.Expressions;

import static com.umbrella.logical.fn.ExprFunction.*;

public class Main {
    public static void main(String[] args) {
        var ctx = ExecutionContext.instance();
        var df = ctx.parquet("file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet")
                .select(col("s_suppkey"), col("s_name"))
                .filter(eq(col("s_name"), lit("Mike")))
                .orderBy(asc(col("s_name")))
                .limit(100);
        System.out.println(df.schema().toJson());
        df.explain();
        ctx.stop();
    }
}
