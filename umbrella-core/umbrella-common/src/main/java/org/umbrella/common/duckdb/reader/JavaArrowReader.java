package org.umbrella.common.duckdb.reader;

import lombok.SneakyThrows;
import org.apache.arrow.adapter.jdbc.ArrowVectorIterator;
import org.apache.arrow.adapter.jdbc.JdbcToArrow;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfigBuilder;
import org.apache.arrow.adapter.jdbc.JdbcToArrowUtils;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.util.AutoCloseables;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class JavaArrowReader extends ArrowReader {
    private final Schema schema;
    private ArrowVectorIterator iterator;
    private ResultSet resultSet;

    @SneakyThrows
    public <E> JavaArrowReader(BufferAllocator allocator, Collection<E> collection, Class<E> clazz) {
        super(allocator);

        var fields = DataTypeConverter.fields(clazz);
        var create = DSL.using(SQLDialect.DUCKDB);
        var result = create.newResult(fields);

        for (E e : collection) {
            var record = create.newRecord(fields);
            record.from(e);
            result.add(record);
        }

        var config = new JdbcToArrowConfigBuilder(allocator, JdbcToArrowUtils.getUtcCalendar())
                .setReuseVectorSchemaRoot(true)
                .setIncludeMetadata(true)
                .setTargetBatchSize(collection.size() + 1)
                .build();
        try {
            resultSet = result.intoResultSet();
            iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, config);
            schema = JdbcToArrowUtils.jdbcToArrowSchema(resultSet.getMetaData(), config);
        } catch (SQLException | IOException e) {
            if (iterator != null) AutoCloseables.close(e, iterator);
            if (resultSet != null) AutoCloseables.close(e, resultSet);
            throw new DataAccessException("创建 JDBC Arrow Reader 出错.", e);
        }
    }

    @Override
    public boolean loadNextBatch() throws IOException {
        if (!iterator.hasNext()) return false; // fast return

        prepareLoadNextBatch();
        final VectorUnloader unloader = new VectorUnloader(iterator.next());
        loadRecordBatch(unloader.getRecordBatch());
        return true;
    }

    @Override
    public long bytesRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SneakyThrows
    protected void closeReadSource() {
        iterator.close();
        resultSet.close();
    }

    @Override
    protected Schema readSchema() {
        return schema;
    }
}
