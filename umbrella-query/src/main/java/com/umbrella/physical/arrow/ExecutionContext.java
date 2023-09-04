package com.umbrella.physical.arrow;

import com.google.common.collect.Iterables;
import com.umbrella.physical.arrow.expr.*;
import com.umbrella.physical.arrow.plan.*;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.types.Types;
import org.apache.calcite.adapter.arrow.ArrowTable;
import org.apache.calcite.interpreter.Bindables;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.logical.LogicalSort;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.SqlTypeName;

import java.util.Objects;
import java.util.Optional;

public final class ExecutionContext {
    private ExecutionContext(){
        allocator = new RootAllocator();
    }

    private final BufferAllocator allocator;

    private volatile static ExecutionContext instance;

    public static ExecutionContext instance() {
        if(Objects.isNull(instance)) {
            synchronized (ExecutionContext.class) { // can't use instance as lock for it is null here
                if(Objects.isNull(instance)) {
                    instance = new ExecutionContext();
                }
            }
        }
        return instance;
    }

    public BufferAllocator allocator() {
        return allocator;
    }

    private PhysicalExpr createPhysicalExpr(RexNode node) {
        if(node instanceof RexLiteral n) {
            if(n.getType().toString().equals(SqlTypeName.INTEGER.getName())) {
                return new LiteralExpr.Int(n.getValueAs(Integer.class));
            } else if (n.getType().toString().equals(SqlTypeName.BIGINT.getName())) {
                return new LiteralExpr.Long(n.getValueAs(Long.class));
            } else if (n.getType().toString().equals(SqlTypeName.FLOAT.getName())) {
                return new LiteralExpr.Float(n.getValueAs(Float.class));
            } else if (n.getType().toString().equals(SqlTypeName.DOUBLE.getName())) {
                return new LiteralExpr.Double(n.getValueAs(Double.class));
            } else if (n.getType().toString().equals(SqlTypeName.BOOLEAN.getName())) {
                return new LiteralExpr.Bool(n.getValueAs(Boolean.class));
            } else if (n.getType().toString().equals(SqlTypeName.VARCHAR.getName())) {
                return new LiteralExpr.String(n.getValueAs(String.class));
            } else if (n.getTypeName() == SqlTypeName.CHAR) {
                return new LiteralExpr.String(n.getValueAs(String.class));
            } else if (n.getTypeName() == SqlTypeName.DECIMAL) {
                return new LiteralExpr.Float(n.getValueAs(Float.class));
            } else {
                throw new UnsupportedOperationException("SqlType " + n.getType().toString() + " is not supported");
            }
        } else if (node instanceof RexCall n) {
            if(n.operands.size() == 1) {
                var op = createPhysicalExpr(n.operands.get(0));
                if (n.isA(SqlKind.CAST)) {
                    Types.MinorType type;
                    if(n.getType().toString().equals(SqlTypeName.INTEGER.getName())) {
                        type = Types.MinorType.INT;
                    } else if (n.getType().toString().equals(SqlTypeName.BIGINT.getName())) {
                        type = Types.MinorType.BIGINT;
                    } else if (n.getType().toString().equals(SqlTypeName.FLOAT.getName())) {
                        type = Types.MinorType.FLOAT4;
                    } else if (n.getType().toString().equals(SqlTypeName.DOUBLE.getName())) {
                        type = Types.MinorType.FLOAT8;
                    } else if (n.getType().toString().equals(SqlTypeName.BOOLEAN.getName())) {
                        type = Types.MinorType.BIT;
                    } else if (n.getType().toString().equals(SqlTypeName.VARCHAR.getName())) {
                        type = Types.MinorType.VARCHAR;
                    } else {
                        throw new UnsupportedOperationException("SqlType " + n.getType().toString() + " is not supported");
                    }
                    return new CastExpr(op, type);
                } else {
                    throw new UnsupportedOperationException("RexCall " + n.getKind() + " is not supported");
                }
            } else if (n.operands.size() == 2) {
                var l = createPhysicalExpr(n.operands.get(0));
                var r = createPhysicalExpr(n.operands.get(1));
                if (n.isA(SqlKind.PLUS)) {
                    return new MathExpr.Add(l, r);
                } else if (n.isA(SqlKind.MINUS)) {
                    return new MathExpr.Sub(l, r);
                } else if (n.isA(SqlKind.TIMES)) {
                    return new MathExpr.Mul(l, r);
                } else if (n.isA(SqlKind.DIVIDE)) {
                    return new MathExpr.Div(l, r);
                } else if (n.isA(SqlKind.MOD)) {
                    return new MathExpr.Mod(l, r);
                } else if (n.isA(SqlKind.GREATER_THAN_OR_EQUAL)) {
                    return new BooleanExpr.Ge(l, r);
                } else {
                    throw new UnsupportedOperationException("RexCall " + n.getKind() + " is not supported");
                }
            } else {
                throw new IllegalStateException("RexCall 没有操作表达式");
            }
        } else if (node instanceof RexInputRef n) {
            return new ColumnExpr(n.getIndex());
        } else throw new UnsupportedOperationException("RexNode " + node.getClass().getName() + " is not supported");
    }

    public PhysicalPlan createPhysicalPlan(RelNode node) {
        if(node instanceof LogicalTableScan n) {
            var table = n.getTable();
            ArrowTable arrowTable = table.unwrap(ArrowTable.class);
            return new PhysicalTableScan(arrowTable.getUri(), arrowTable.getFormat(), Optional.empty());
        } else if (node instanceof Bindables.BindableTableScan n) {
            var table = n.getTable();
            var rowType = table.getRowType();
            var project = Iterables.toArray(n.projects.stream().map(i -> rowType.getFieldNames().get(i)).toList(), String.class);
            ArrowTable arrowTable = table.unwrap(ArrowTable.class);
            return new PhysicalTableScan(arrowTable.getUri(), arrowTable.getFormat(), Optional.of(project));
        } else if (node instanceof LogicalProject n) {
            return new PhysicalProject(createPhysicalPlan(n.getInput()), n.getProjects().stream().map(this::createPhysicalExpr).toList());
        } else if (node instanceof LogicalSort n) {
            var offset = n.offset == null ? 0 : ((RexLiteral) n.offset).getValueAs(Integer.class);
            var fetch = n.fetch == null ? 0 : ((RexLiteral) n.fetch).getValueAs(Integer.class);
            var input = createPhysicalPlan(n.getInput());
            return new PhysicalSort(input, offset, fetch);
        } else if (node instanceof LogicalFilter n) {
            var input = createPhysicalPlan(n.getInput());
            var expr = createPhysicalExpr(n.getCondition());
            return new PhysicalFilter(input, expr);
        } else throw new UnsupportedOperationException("RelNode " + node.getRelTypeName() + " is not supported");
    }

    public void close() {
        allocator().close();
    }
}
