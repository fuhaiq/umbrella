package org.umbrella.query;

public interface EngineCacheHandler extends EngineHandler {
    void evict();
}
