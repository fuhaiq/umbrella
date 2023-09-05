package com.umbrella;

import com.umbrella.logical.Optimizer;
import com.umbrella.physical.arrow.ExecutionContext;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.calcite.adapter.arrow.ArrowSchema;
import org.apache.calcite.adapter.arrow.ArrowTable;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.sql.parser.SqlParseException;

import java.util.List;

public class Main {
    public static void main(String[] args) throws SqlParseException {

        var sql = """
                select s_suppkey,s_acctbal+0.002
                from supplier
                where s_acctbal = 9915.24 and s_suppkey <> 762414
                order by s_name desc, s_acctbal asc, s_suppkey desc
                limit 10
                """;

        //[s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment]
        var fileName = "file:/Users/haiqing.fu/Downloads/part-00000-6428eccd-bba8-4976-bceb-e45c2aafe709-c000.snappy.parquet";

        var schema = ArrowSchema.newBuilder("ods").addTable("supplier",
                new ArrowTable(fileName,
                        FileFormat.PARQUET)).build();

        var optimizer = Optimizer.create(schema, List.of(
                CoreRules.PROJECT_TABLE_SCAN
//                CoreRules.FILTER_SCAN
        ));
        // 1. SQL parse: SQL string --> SqlNode
        var sqlNode = optimizer.parse(sql);
        System.out.println(sqlNode);

        // 2. SQL validate: SqlNode --> SqlNode
        var validateSqlNode = optimizer.validate(sqlNode);
        System.out.println(validateSqlNode);

        // 3. SQL convert: SqlNode --> RelNode
        var relNode = optimizer.convert(validateSqlNode);
        System.out.println(relNode);

        // 4. SQL Optimize: RelNode --> RelNode
        var optimizerRelTree = optimizer.optimize(
                relNode,
                relNode.getTraitSet());
        System.out.println(optimizerRelTree);

        var engine = ExecutionContext.instance();
        var physicalPlan = engine.createPhysicalPlan(optimizerRelTree);
        System.out.println(physicalPlan.explain());
        try(var ret = physicalPlan.execute()) {
            System.out.println(ret.contentToTSVString(optimizerRelTree.getRowType()));
        }
        engine.close();
    }
}
