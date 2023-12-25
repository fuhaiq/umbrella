package org.umbrella.query.session;

import java.io.Closeable;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

public record EngineSessionResource(Connection conn, List<AutoCloseable> resources) implements Closeable {

    public void addResource(AutoCloseable... resource) {
        resources.addAll(Arrays.asList(resource));
    }

    @Override
    public void close() {
        resources.forEach(r -> {
            try {
                r.close();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }
}
