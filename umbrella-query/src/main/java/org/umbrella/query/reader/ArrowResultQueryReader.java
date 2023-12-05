package org.umbrella.query.reader;

import org.apache.arrow.adapter.jdbc.*;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.jooq.ResultQuery;
import org.jooq.exception.DataAccessException;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * 1. 方便传入 {@link ResultQuery}, 会自动关闭与之关联的 {@link ResultSet}
 * <br>
 * 2. {@link ArrowResultQueryReader#loadNextBatch()} 设置了 reuseVectorSchemaRoot, 不用 try-resource 关闭 root, iterator.close 会清理的
 */
public class ArrowResultQueryReader extends ArrowReader {
    private ArrowVectorIterator iterator;
    private final Schema schema;
    private final ResultSet resultSet;
    public ArrowResultQueryReader(BufferAllocator allocator, ResultQuery<?> resultQuery) {
        this(new JdbcToArrowConfigBuilder(allocator, JdbcToArrowUtils.getUtcCalendar())
                .setReuseVectorSchemaRoot(true)
                .build(), resultQuery);
    }

    public ArrowResultQueryReader(JdbcToArrowConfig config, ResultQuery<?> resultQuery) {
        super(config.getAllocator());
        checkArgument(config.isReuseVectorSchemaRoot(), "目前只支持 reuseVectorSchemaRoot = true");
        resultSet = resultQuery.fetchResultSet();
        try {
            iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, config);
            schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);
        } catch (SQLException | IOException e) {
            if(iterator != null) AutoCloseables.close(e, iterator);
            AutoCloseables.close(e, resultSet);
            throw new DataAccessException("创建 JDBC Arrow Reader 出错.", e);
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
