package com.umbrella.physical.arrow;

import com.google.common.collect.Iterables;
import com.umbrella.physical.arrow.expr.*;
import com.umbrella.physical.arrow.plan.PhysicalPlan;
import com.umbrella.physical.arrow.plan.PhysicalProject;
import com.umbrella.physical.arrow.plan.PhysicalSort;
import com.umbrella.physical.arrow.plan.PhysicalTableScan;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.calcite.adapter.arrow.ArrowTable;
import org.apache.calcite.interpreter.Bindables;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.logical.LogicalSort;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;

import java.math.BigDecimal;
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

    public void stop() {
        allocator.close();
    }

    public PhysicalExpr createPhysicalExpr(RexNode node) {
        assert node != null;
        switch (node) {
            case RexLiteral n -> {
                if (n.getTypeName() == SqlTypeName.INTEGER) {
                    return new LiteralInt(n.getValueAs(Integer.class));
                } else if (n.getTypeName() == SqlTypeName.BIGINT) {
                    return new LiteralLong(n.getValueAs(Long.class));
                } else if (n.getTypeName() == SqlTypeName.FLOAT) {
                    return new LiteralFloat(n.getValueAs(Float.class));
                } else if (n.getTypeName() == SqlTypeName.DOUBLE) {
                    return new LiteralDouble(n.getValueAs(Double.class));
                } else if (n.getTypeName() == SqlTypeName.BOOLEAN) {
                    return new LiteralBool(n.getValueAs(Boolean.class));
                } else if (n.getTypeName() == SqlTypeName.VARCHAR) {
                    return new LiteralString(n.getValueAs(String.class));
                } else if (n.getTypeName() == SqlTypeName.CHAR) {
                    return new LiteralString(n.getValueAs(String.class));
                } else if (n.getTypeName() == SqlTypeName.DECIMAL) {
                    return new DecimalExpr(n.getValueAs(BigDecimal.class));
                } else {
                    throw new UnsupportedOperationException("SqlType " + n.getTypeName() + " is not supported");
                }
            }
            case RexInputRef n -> {
                return new ColumnExpr(n.getIndex());
            }
            case null, default -> {
                throw new UnsupportedOperationException("RexNode " + node.getClass().getName() + " is not supported");
            }
        }
    }

    public PhysicalPlan createPhysicalPlan(RelNode node) {
        assert node != null;
        switch (node) {
            case LogicalTableScan n -> {
                var table = n.getTable();
                ArrowTable arrowTable = table.unwrap(ArrowTable.class);
                return new PhysicalTableScan(arrowTable.getUri(), arrowTable.getFormat(), Optional.empty());
            }
            case Bindables.BindableTableScan n -> {
                var table = n.getTable();
                var rowType = table.getRowType();
                var list = Iterables.toArray(n.projects.stream().map(i -> rowType.getFieldNames().get(i)).toList(), String.class);
                ArrowTable arrowTable = table.unwrap(ArrowTable.class);
                return new PhysicalTableScan(arrowTable.getUri(), arrowTable.getFormat(), Optional.of(list));
            }
            case LogicalProject n -> {
                return new PhysicalProject(createPhysicalPlan(n.getInput()), n.getProjects().stream().map(this::createPhysicalExpr).toList());
            }
            case LogicalSort n -> {
                var offset = n.offset == null ? 0 : ((RexLiteral) n.offset).getValueAs(Integer.class);
                var fetch = n.fetch == null ? 0 : ((RexLiteral) n.fetch).getValueAs(Integer.class);
                return new PhysicalSort(createPhysicalPlan(n.getInput()), offset, fetch);
            }
            case null, default ->
                    throw new UnsupportedOperationException("RelNode " + node.getRelTypeName() + " is not supported");
        }
    }
}
