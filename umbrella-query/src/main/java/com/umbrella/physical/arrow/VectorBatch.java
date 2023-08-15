package com.umbrella.physical.arrow;

import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.util.TransferPair;
import org.apache.calcite.rel.type.RelDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class VectorBatch implements AutoCloseable {
    private final List<FieldVector> fieldVectors;
    private final int rowCount;
    private final int columnCount;

    private VectorBatch(List<FieldVector> fieldVectors, int rowCount, int columnCount) {
        this.fieldVectors = fieldVectors;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }

    public static VectorBatch of(FieldVector... vectors) {
        checkArgument(vectors != null && vectors.length > 0, "初始化 FieldVector 不能为空");
        var v = Arrays.stream(vectors).toList();
        return new VectorBatch(v, v.size() == 0 ? 0 : v.get(0).getValueCount(), v.size());
    }

    public static VectorBatch of(List<FieldVector> vectors) {
        checkArgument(vectors != null && vectors.size() > 0, "初始化 FieldVector 不能为空");
        return new VectorBatch(vectors, vectors.get(0).getValueCount(), vectors.size());
    }

    public FieldVector getVector(int index) {
        checkArgument(index >= 0 && index < fieldVectors.size(), "下标越界");
        return fieldVectors.get(index);
    }

    public <T extends FieldVector> T getVector(int index, Class<T> clazz) {
        checkArgument(index >= 0 && index < fieldVectors.size(), "下标越界");
        return clazz.cast(fieldVectors.get(index));
    }

    public VectorBatch slice(int index, int length) {
        checkArgument(index >= 0, "起始位置必须 >=0");
        checkArgument(length >= 0, "截取长度必须 >=0");
        checkArgument(index + length <= rowCount, "越界,起始位置+截取长度必须 <= rowCount");

        if (index == 0 && length == rowCount) {
            return this;
        }

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

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
    }
}
