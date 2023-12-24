package org.umbrella.query;

import org.umbrella.query.session.EngineSession;

import java.util.function.Function;

public interface QueryEngine {
    <T> T session(Function<EngineSession, T> func);
    EngineWriter write();
    default EngineCacheHandler cache(String name) {
        return cache("cache", name);
    }
    EngineCacheHandler cache(String schema, String name);
    EngineReader reader();
}
