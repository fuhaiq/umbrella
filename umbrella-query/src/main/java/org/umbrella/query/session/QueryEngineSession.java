package org.umbrella.query.session;

import org.jooq.ResultQuery;

import java.io.Closeable;

public interface QueryEngineSession extends Closeable {
    void start();

    void jdbc(String tableName, ResultQuery<?> rq);

    void orc(String tableName, String uri);

    void avro(String tableName, String uri);

    @Override
    void close();
}
