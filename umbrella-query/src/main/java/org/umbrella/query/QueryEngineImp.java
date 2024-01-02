package org.umbrella.query;

import org.umbrella.query.cache.EngineCacheHandler;
import org.umbrella.query.cache.EngineCacheHandlerImp;
import org.umbrella.query.session.EngineSession;

import java.util.function.Function;

public record QueryEngineImp(EngineClient client, EngineSession session) implements QueryEngine {

  @Override
  public <T> T session(Function<EngineSession, T> func) {
    try (session) {
      session.start();
      return func.apply(session);
    }
  }

  @Override
  public EngineWriter write() {
    return client.writer();
  }

  @Override
  public EngineCacheHandler cache(String schema, String name) {
    return new EngineCacheHandlerImp(schema, name, client);
  }

  @Override
  public EngineReader reader() {
    return client.reader();
  }
}
