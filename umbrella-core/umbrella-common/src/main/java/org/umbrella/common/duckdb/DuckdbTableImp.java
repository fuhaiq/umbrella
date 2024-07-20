package org.umbrella.common.duckdb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.c.Data;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.commons.lang3.RandomStringUtils;
import org.duckdb.DuckDBConnection;
import org.jooq.exception.DataAccessException;
import org.umbrella.common.duckdb.mapper.DuckdbArrowMapper;
import org.umbrella.common.duckdb.reader.JavaArrowReader;
import org.umbrella.common.duckdb.reader.RootArrowReader;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
public class DuckdbTableImp implements DuckdbTable, AutoCloseable{
    private final String name;
    private final BufferAllocator allocator;
    private final DuckdbSessionResource resource;

    @Override
    public <T> void from(Class<T> clazz, Collection<T> collection) {
        arrow(new JavaArrowReader(allocator, collection, clazz));
    }

    @Override
    public <T> void from(Class<T> clazz, T obj) {
        arrow(new JavaArrowReader(allocator, List.of(obj), clazz));
    }

    @Override
    public void from(VectorSchemaRoot root) {
        arrow(RootArrowReader.fromRoot(allocator, root));
    }

    private void arrow(ArrowReader arrowReader) {
        try {
            var stream = ArrowArrayStream.allocateNew(allocator);
            resource.addResource(stream, arrowReader);
            var sqlSession = resource.sqlSession();
            var conn = sqlSession.getConnection();
            checkState(conn.isWrapperFor(DuckDBConnection.class), "引擎驱动不匹配");
            Data.exportArrayStream(allocator, arrowReader, stream);
            var duckConn = conn.unwrap(DuckDBConnection.class);
            var from = RandomStringUtils.randomAlphabetic(10);
            duckConn.registerArrowStream(from, stream);
            if (log.isDebugEnabled()) log.debug("数据导出 Arrow 会话完成.");
            var mapper = sqlSession.getMapper(DuckdbArrowMapper.class);
            mapper.create(from, name);
            if (log.isDebugEnabled()) log.debug("数据导入 duckdb 完成.");
            resource.addResource(this);
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        var mapper = resource.sqlSession().getMapper(DuckdbArrowMapper.class);
        mapper.drop(name);
        if (log.isDebugEnabled()) log.debug("清除 duckdb 数据完成.");
    }
}
