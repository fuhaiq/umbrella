package com.umbrella;

import com.umbrella.execution.ExecutionContext;
import static com.umbrella.logical.fn.ExprFunction.*;

public class Main {
    public static void main(String[] args) {
        var ctx = new ExecutionContext();

        var df = ctx.parquet("file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet")
                .select(col("s_suppkey"), col("s_name"))
                .filter(eq(col("s_suppkey"), lit(100)));
        df.explain();
    }
}
