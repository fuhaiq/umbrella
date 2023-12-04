package org.umbrella.query.reader;

import org.apache.arrow.adapter.jdbc.ArrowVectorIterator;
import org.apache.arrow.adapter.jdbc.JdbcToArrow;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfigBuilder;
import org.apache.arrow.adapter.jdbc.JdbcToArrowUtils;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.jooq.ResultQuery;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 1. 方便传入 {@link ResultQuery}, 会自动关闭与之关联的 {@link ResultSet}
 * <br>
 * 2. {@link ArrowResultQueryReader#loadNextBatch()} 设置了 reuseVectorSchemaRoot, 不用 try-resource 关闭 root, iterator.close 会清理的
 */
public class ArrowResultQueryReader extends ArrowReader {
    private ArrowVectorIterator iterator;
    private Schema schema;
    private ResultSet resultSet;
    private final ResultQuery<?> resultQuery;
    public ArrowResultQueryReader(BufferAllocator allocator, ResultQuery<?> resultQuery) {
        super(allocator);
        this.resultQuery = resultQuery;
    }

    @Override
    protected void initialize() throws IOException {
        super.initialize();
        resultSet = resultQuery.fetchResultSet();
        final var config = new JdbcToArrowConfigBuilder(allocator, JdbcToArrowUtils.getUtcCalendar())
                .setReuseVectorSchemaRoot(true)
                .build();
        try {
            iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, config);
            schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);
        } catch (SQLException e) {
            if(iterator != null) AutoCloseables.close(e, iterator);
            throw new IOException("创建 JDBC Arrow Reader 出错.", e);
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
    protected void closeReadSource() throws IOException {
        try {
            iterator.close();
            resultSet.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Schema readSchema() {
        return schema;
    }
}
