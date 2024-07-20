package org.umbrella.common.duckdb;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
public class DuckdbSessionImpl implements DuckdbSession {

    private final BufferAllocator allocator;
    private final SqlSessionFactory sqlSessionFactory;
    private final ThreadLocal<DuckdbSessionResource> local = new ThreadLocal<>();

    @Override
    @SneakyThrows
    public void start() {
        checkState(local.get() == null, "开启 DuckDB 会话失败,会话已经开启.");
        DynamicDataSourceContextHolder.push("duckdb");
        var sqlSession = sqlSessionFactory.openSession();
        local.set(new DuckdbSessionResource(sqlSession, new ArrayList<>()));
        if (log.isDebugEnabled()) log.debug("开启 DuckDB 会话");
    }

    @Override
    public <T> T mapper(Class<T> mapper) {
        var resource = local.get();
        checkNotNull(local.get(), "获取 DuckDB 会话失败,会话未开启.");
        return resource.sqlSession().getMapper(mapper);
    }

    @Override
    public DuckdbTable define(String name) {
        checkNotNull(local.get(), "获取 DuckDB 会话失败,会话未开启.");
        return new DuckdbTableImp(name, allocator, local.get());
    }

    @Override
    public void close() {
        checkNotNull(local.get(), "关闭 DuckDB 会话失败,会话已经关闭.");
        var resource = local.get();
        try {
            resource.close();
        } finally {
            resource.sqlSession().close();
            local.remove();
            DynamicDataSourceContextHolder.clear();
        }
        if (log.isDebugEnabled()) log.debug("关闭 DuckDB 会话");
    }
}
