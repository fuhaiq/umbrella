package com.umbrella;

import com.umbrella.calcite.SchemaMapDataContext;
import com.umbrella.calcite.adapter.SchemaBasedFileTable;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.calcite.adapter.enumerable.EnumerableInterpretable;
import org.apache.calcite.adapter.enumerable.EnumerableRel;
import org.apache.calcite.adapter.enumerable.EnumerableRules;
import org.apache.calcite.interpreter.Bindables;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.tools.*;

import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        var schema = CalciteSchema.createRootSchema(true);

//        var fileName = "file:///C:\\work\\user.parquet";

        //[s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment]
        var fileName = "file:/Users/haiqing.fu/Downloads/part-00000-f789dcc2-3a14-4651-bbc2-ea8fbf76c829-c000.snappy.parquet";

        schema.add("user",
                new SchemaBasedFileTable(fileName,
                        FileFormat.PARQUET));
        // inject schema
        var configBuilder = Frameworks.newConfigBuilder();
        configBuilder.defaultSchema(schema.plus());
        var config = configBuilder.build();

        var df = RelBuilder.create(config);
        var logicalPlan = df.scan("user").
                project(df.field("s_name"), df.field("s_suppkey"), df.field("s_phone")).
//                filter(df.call(SqlStdOperatorTable.GREATER_THAN,
//                        df.field("s_suppkey"),
//                        df.literal(100))).
                build();

        System.out.println(logicalPlan.explain());

        // optimize
        var program = new HepProgramBuilder().addRuleCollection(List.of(
                CoreRules.PROJECT_TABLE_SCAN,
                CoreRules.FILTER_SCAN,
//                CoreRules.FILTER_TO_CALC,
//                CoreRules.PROJECT_TO_CALC,
//                CoreRules.FILTER_CALC_MERGE,
//                CoreRules.PROJECT_CALC_MERGE,
//                CoreRules.FILTER_INTO_JOIN     // 过滤谓词下推到Join之前
                EnumerableRules.ENUMERABLE_TABLE_SCAN_RULE
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

        var bindable = (Bindables.BindableTableScan) optimizedPlan;

//        var enumerable = (EnumerableRel) optimizedPlan;
//
//        var bindable = EnumerableInterpretable.toBindable(new HashMap<>(),
//                null, enumerable, EnumerableRel.Prefer.ARRAY);

        var bind = bindable.bind(new SchemaMapDataContext(schema.plus()));

        try(var enumerator = bind.enumerator()){
            StringBuilder sb = new StringBuilder();
            var count = 0;
            while (enumerator.moveNext()) {
                Object current = enumerator.current();
                Object[] values = (Object[]) current;
                for (Object v : values) {
                    sb.append(v).append(",");
                }
                sb.setLength(sb.length() - 1);
                System.out.println(sb);
                sb.delete(0, sb.length()-1);
            }
        }
    }
}
