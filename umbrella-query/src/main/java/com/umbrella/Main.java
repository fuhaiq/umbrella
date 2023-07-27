package com.umbrella;

import com.umbrella.calcite.adapter.SchemaFileTable;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

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

        // define database
        var database = CalciteSchema.createRootSchema(true);
        database.add("customer",
                new SchemaFileTable("file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet",
                        FileFormat.PARQUET));

        // inject schema
        var configBuilder = Frameworks.newConfigBuilder();
        configBuilder.defaultSchema(database.plus());

        var df = RelBuilder.create(configBuilder.build());
        var r = df.scan("customer").
                project(df.field("s_name"), df.field("s_nationkey"), df.field("s_acctbal")).
                filter(df.call(SqlStdOperatorTable.GREATER_THAN,
                        df.field("s_nationkey"),
                        df.literal(100))).
                build();
        System.out.println(RelOptUtil.toString(r));
    }
}
