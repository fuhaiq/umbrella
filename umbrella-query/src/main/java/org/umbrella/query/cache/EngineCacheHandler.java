package org.umbrella.query.cache;

import org.umbrella.query.EngineHandler;

public interface EngineCacheHandler extends EngineHandler {
  void evict();
}
