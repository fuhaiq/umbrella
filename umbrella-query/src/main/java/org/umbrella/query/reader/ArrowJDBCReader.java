package org.umbrella.query.reader;

import org.apache.arrow.adapter.jdbc.*;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkArgument;

public class ArrowJDBCReader extends ArrowReader {
    private final ArrowVectorIterator iterator;
    private final Schema schema;
    public ArrowJDBCReader(BufferAllocator allocator, ResultSet resultSet) throws SQLException, IOException {
        this(resultSet, new JdbcToArrowConfigBuilder(allocator, JdbcToArrowUtils.getUtcCalendar())
                .setReuseVectorSchemaRoot(true)
                .build());
    }

    public ArrowJDBCReader(ResultSet resultSet, JdbcToArrowConfig config) throws SQLException, IOException {
        super(config.getAllocator());
        checkArgument(config.isReuseVectorSchemaRoot(), "目前只支持 reuseVectorSchemaRoot = true");
        this.iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, config);
        this.schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if(!iterator.hasNext()) return false; // fast return

        prepareLoadNextBatch();
        var root = iterator.next(); // 因为设置了 reuseVectorSchemaRoot, 这里不用 try-resource 关闭 root, iterator.close 会清理的
        final VectorUnloader unloader = new VectorUnloader(root);
        loadRecordBatch(unloader.getRecordBatch());
        return true;
    }

    @Override
    public long bytesRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void closeReadSource() throws IOException {
        iterator.close();
    }

    @Override
    protected Schema readSchema() throws IOException {
        return schema;
    }
}
