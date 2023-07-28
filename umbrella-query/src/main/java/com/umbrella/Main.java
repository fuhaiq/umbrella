package com.umbrella;

import com.umbrella.calcite.adapter.SchemaBasedFileSchema;
import com.umbrella.calcite.adapter.SchemaBasedFileTable;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.calcite.jdbc.CalciteSchema;
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

        // define databases
        var databases = CalciteSchema.createRootSchema(true);

        // define schame

        // [registration_dttm, id, first_name, last_name, email, gender, ip_address, cc, country, birthdate, salary, title, comments]
        var schema = new SchemaBasedFileSchema("ods");
        schema.addTable("user",
                new SchemaBasedFileTable("file:///C:\\work\\user.parquet",
                        FileFormat.PARQUET));
        // add schema
        databases.add(schema.getName(), schema);


        // inject schema
        var configBuilder = Frameworks.newConfigBuilder();
        configBuilder.defaultSchema(databases.getSubSchema(schema.getName(), false).plus());

        var df = RelBuilder.create(configBuilder.build());
        var r = df.scan("user").
                project(df.field("first_name"), df.field("last_name"), df.field("title")).
//                filter(df.call(SqlStdOperatorTable.GREATER_THAN,
//                        df.field("s_nationkey"),
//                        df.literal(100))).
                build();
        System.out.println(r.explain());
    }
}
