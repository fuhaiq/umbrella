package org.umbrella.common.duckdb.mapper;

public interface DuckdbArrowMapper {

    void create(String from, String to);

    void drop(String name);

}
