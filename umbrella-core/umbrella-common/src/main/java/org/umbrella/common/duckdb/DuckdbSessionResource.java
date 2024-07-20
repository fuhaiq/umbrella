package org.umbrella.common.duckdb;

import org.apache.ibatis.session.SqlSession;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

public record DuckdbSessionResource(SqlSession sqlSession, List<AutoCloseable> resources)
        implements Closeable {

    public void addResource(AutoCloseable... resource) {
        resources.addAll(Arrays.asList(resource));
    }

    @Override
    public void close() {
        resources.forEach(
                r -> {
                    try {
                        r.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
    }
}
