package org.umbrella.query.reader;

import org.apache.arrow.adapter.jdbc.*;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * 1. 需要客户端手动关闭 {@link ResultSet}
 * <br/>
 * 2. {@link ArrowJDBCReader#loadNextBatch()} 设置了 reuseVectorSchemaRoot, 不用 try-resource 关闭 root, iterator.close 会清理的
 */
public class ArrowJDBCReader extends ArrowReader {
    private ArrowVectorIterator iterator;
    private final Schema schema;
    public ArrowJDBCReader(BufferAllocator allocator, ResultSet resultSet) {
        this(resultSet, new JdbcToArrowConfigBuilder(allocator, JdbcToArrowUtils.getUtcCalendar())
                .setReuseVectorSchemaRoot(true)
                .build());
    }

    public ArrowJDBCReader(ResultSet resultSet, JdbcToArrowConfig config) {
        super(config.getAllocator());
        checkArgument(config.isReuseVectorSchemaRoot(), "目前只支持 reuseVectorSchemaRoot = true");
        try {
            this.iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, config);
            this.schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);
        } catch (SQLException | IOException e) {
            if(iterator != null) AutoCloseables.close(e, iterator);
            throw new RuntimeException("创建 JDBC Arrow Reader 出错.", e);
        }
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if(!iterator.hasNext()) return false; // fast return

        prepareLoadNextBatch();
        var root = iterator.next();
        final VectorUnloader unloader = new VectorUnloader(root);
        loadRecordBatch(unloader.getRecordBatch());
        return true;
    }

    @Override
    public long bytesRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void closeReadSource() {
        iterator.close();
    }

    @Override
    protected Schema readSchema() {
        return schema;
    }
}
