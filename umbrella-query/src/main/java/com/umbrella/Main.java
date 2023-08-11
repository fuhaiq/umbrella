package com.umbrella;

import com.umbrella.physical.arrow.plan.PhysicalTableScan;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.calcite.adapter.arrow.ArrowTable;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.*;

import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        var schema = CalciteSchema.createRootSchema(true);

        //[s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment]
        var fileName = "file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet";

        schema.add("supplier",
                new ArrowTable(fileName,
                        FileFormat.PARQUET));
        // inject schema
        var configBuilder = Frameworks.newConfigBuilder();
        configBuilder.defaultSchema(schema.plus());
        var config = configBuilder.build();

        var df = RelBuilder.create(config);
        var logicalPlan = df.scan("supplier").
                project(df.alias(df.literal("HELLO"), "msg"), df.field("s_comment"),df.field("s_nationkey"), df.field("s_suppkey"), df.field("s_phone")).
                filter(df.call(SqlStdOperatorTable.GREATER_THAN,
                        df.field("s_phone"),
                        df.literal(100))).
                filter(
                        df.or(
                                df.call(SqlStdOperatorTable.LESS_THAN,
                                        df.field("s_phone"),
                                        df.literal(200)),
                                df.call(SqlStdOperatorTable.EQUALS,
                                        df.field("s_nationkey"),
                                        df.literal(10))
                        )
                ).
                limit(0, 100).
                build();

        System.out.println(logicalPlan.explain());

        // optimize
        var program = new HepProgramBuilder().addRuleCollection(List.of(
                CoreRules.PROJECT_TABLE_SCAN,
                CoreRules.FILTER_SCAN,
                CoreRules.FILTER_MERGE,
//                CoreRules.FILTER_TO_CALC,
//                CoreRules.PROJECT_TO_CALC,
//                CoreRules.FILTER_CALC_MERGE,
//                CoreRules.PROJECT_CALC_MERGE,
                CoreRules.FILTER_INTO_JOIN     // 过滤谓词下推到Join之前
//                EnumerableRules.ENUMERABLE_TABLE_SCAN_RULE
//                EnumerableRules.ENUMERABLE_PROJECT_RULE,
//                EnumerableRules.ENUMERABLE_FILTER_RULE
//                EnumerableRules.ENUMERABLE_PROJECT_TO_CALC_RULE,
//                EnumerableRules.ENUMERABLE_FILTER_TO_CALC_RULE,
//                EnumerableRules.ENUMERABLE_JOIN_RULE,
//                EnumerableRules.ENUMERABLE_SORT_RULE,
//                EnumerableRules.ENUMERABLE_CALC_RULE,
//                EnumerableRules.ENUMERABLE_AGGREGATE_RULE
        )).build();

        var planner = new HepPlanner(program);
        planner.setRoot(logicalPlan);

        var optimizedPlan = planner.findBestExp();

        System.out.println(optimizedPlan.explain());

//        VectorSchemaRoot execute = null;
//        try (
//                var allocator = new RootAllocator();
//                var factory = new FileSystemDatasetFactory(allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, fileName);
//        ) {
//            var arrowSchema = factory.inspect();
//            var scan = new PhysicalTableScan(fileName, arrowSchema, Optional.of(new String[]{"s_address", "s_phone"}));
//            System.out.println(scan.execute().contentToTSVString());
//        }
    }
}
