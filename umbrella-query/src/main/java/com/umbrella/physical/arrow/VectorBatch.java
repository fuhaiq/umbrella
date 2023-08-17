package com.umbrella.physical.arrow;

import com.google.common.collect.ImmutableList;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.util.TransferPair;
import org.apache.calcite.rel.type.RelDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class VectorBatch implements AutoCloseable {
    private final ImmutableList<FieldVector> fieldVectors;
    public final int rowCount;
    public final int columnCount;

    private VectorBatch(List<FieldVector> fieldVectors) {
        this.fieldVectors = ImmutableList.copyOf(fieldVectors);
        this.columnCount = this.fieldVectors.size() == 0 ? 0 : this.fieldVectors.size();
        this.rowCount = columnCount == 0 ? 0 : fieldVectors.get(0).getValueCount();

    }

    public static VectorBatch of(FieldVector... vectors) {
        checkArgument(vectors != null && vectors.length > 0, "初始化 FieldVector 不能为空");
        return VectorBatch.of(Arrays.stream(vectors).toList());
    }

    public static VectorBatch of(List<FieldVector> vectors) {
        checkArgument(vectors != null && vectors.size() > 0, "初始化 FieldVector 不能为空");
        return new VectorBatch(vectors);
    }

    public static VectorBatch of(VectorSchemaRoot root) {
        checkNotNull(root, "参数 VectorSchemaRoot 不能为空");
        return VectorBatch.of(root.getFieldVectors());
    }

    public FieldVector getVector(int index) {
        return fieldVectors.get(index);
    }

    public <T extends FieldVector> T getVector(int index, Class<T> clazz) {
        return clazz.cast(fieldVectors.get(index));
    }

    public VectorBatch slice(int index, int length) {
        checkArgument(index >= 0, "起始位置必须 >=0");
        checkArgument(length >= 0, "截取长度必须 >=0");
        checkArgument(index + length <= rowCount, "越界,起始位置+截取长度必须 <= rowCount");

        var sliceVectors = fieldVectors.stream().map(v -> {
            TransferPair transferPair = v.getTransferPair(v.getAllocator());
            transferPair.splitAndTransfer(index, length);
            return (FieldVector) transferPair.getTo();
        }).toList();

        return VectorBatch.of(sliceVectors);
    }

    @Override
    public void close() {
        try {
            AutoCloseables.close(fieldVectors);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            // should never happen since FieldVector.close() doesn't throw IOException
            throw new RuntimeException(ex);
        }
    }

    private void printRow(StringBuilder sb, List<Object> row) {
        boolean first = true;
        for (Object v : row) {
            if (first) {
                first = false;
            } else {
                sb.append("\t");
            }
            sb.append(v);
        }
        sb.append("\n");
    }

    public String contentToTSVString(RelDataType schema) {
        checkArgument(schema != null, "元数据不能为空");
        checkArgument(schema.getFieldCount() == columnCount, "元数据不匹配");
        StringBuilder sb = new StringBuilder();
        List<Object> row = new ArrayList<>(columnCount);
        row.addAll(schema.getFieldNames());
        printRow(sb, row);
        for (int i = 0; i < rowCount; i++) {
            row.clear();
            for (FieldVector v : fieldVectors) {
                row.add(v.getObject(i));
            }
            printRow(sb, row);
        }
        return sb.toString();
    }
}
