package com.umbrella;

import com.umbrella.execution.ExecutionContext;

import static com.umbrella.logical.fn.ExprFunction.*;

public class Main {
    public static void main(String[] args) {
        var ctx = ExecutionContext.instance();
        var df = ctx.parquet("file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet")
                .select(alias(add(col("s_suppkey"), col("s_name")), "mike"), col("s_suppkey"))
                .filter(and(eq(col("mike"), lit(100)), gt(col("s_suppkey"), lit(100))))
                .orderBy(asc(col("s_name")), desc(col("mike")))
                .limit(100);
        df.explain();
        ctx.stop();
    }
}
