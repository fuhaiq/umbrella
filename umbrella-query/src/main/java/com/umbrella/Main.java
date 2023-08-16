package com.umbrella;

import com.umbrella.logical.Optimizer;
import com.umbrella.physical.arrow.ExecutionContext;
import com.umbrella.physical.arrow.expr.PhysicalExpr;
import com.umbrella.physical.arrow.plan.PhysicalTableScan;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.calcite.adapter.arrow.ArrowSchema;
import org.apache.calcite.adapter.arrow.ArrowTable;
import org.apache.arrow.dataset.file.FileFormat;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.tools.*;

import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) throws SqlParseException {

        var sql = """
                select s_name, s_nationkey, s_nationkey + 1, s_nationkey - 1, s_nationkey *3, 'KKK' from supplier
                """;

        //[s_suppkey, s_name, s_address, s_nationkey, s_phone, s_acctbal, s_comment]
        var fileName = "file:/Users/haiqing.fu/Downloads/part-00000-43eb05ce-afaa-4be3-80b7-5c966f0da9b9-c000.snappy.orc";

        var schema = ArrowSchema.newBuilder("ods").addTable("supplier",
                new ArrowTable(fileName,
                        FileFormat.ORC)).build();

        var optimizer = Optimizer.create(schema, List.of(
                CoreRules.PROJECT_TABLE_SCAN
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
            System.out.println(ret.getColumnCount());
            System.out.println(ret.getRowCount());
        }
    }
}
