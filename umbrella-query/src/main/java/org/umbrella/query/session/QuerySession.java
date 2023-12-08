package org.umbrella.query.session;

import org.jooq.DSLContext;
import org.jooq.ResultQuery;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface QuerySession extends Closeable {
    void start();

    void jdbc(String tableName, ResultQuery<?> rq);

    void jdbc(String tableName, ResultSet rs) throws SQLException;

    void orc(String tableName, String uri);

    void orc(String tableName, String uri, String[] columns);

    void avro(String tableName, String uri);

    void avro(String tableName, String uri, String[] columns);

    DSLContext dsl();

    @Override
    void close();
}
