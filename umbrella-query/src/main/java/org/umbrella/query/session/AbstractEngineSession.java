package org.umbrella.query.session;

import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.umbrella.query.EngineClient;

import java.sql.Connection;

import static cdjd.org.apache.arrow.util.Preconditions.checkState;

@Slf4j
public abstract class AbstractEngineSession implements EngineSession {

  protected final EngineClient client;

  public AbstractEngineSession(EngineClient client) {
    this.client = client;
  }

  abstract EngineSessionResource resource();

  @Override
  public DSLContext dsl() {
    final var element = resource();
    checkState(element != null, "获取 Arrow 会话失败,会话未开启.");
    return DSL.using(element.conn());
  }

  @Override
  public EngineSessionHandler define(String name) {
    checkState(resource() != null, "获取 Arrow 会话失败,会话未开启.");
    return new EngineSessionHandlerImp(name, client, resource());
  }

  @Override
  public void start() {
    checkState(resource() == null, "开启 Arrow 会话失败,会话已经开启.");
    var conn = client.reader().duckdb().configuration().connectionProvider().acquire();
    startSession(conn);
    if (log.isDebugEnabled()) log.debug("开启 Arrow 会话");
  }

  abstract void startSession(Connection conn);

  @Override
  public void close() {
    checkState(resource() != null, "关闭 Arrow 会话失败,会话已经关闭.");
    try (var resource = resource()) {
      closeSession();
      client.reader().duckdb().configuration().connectionProvider().release(resource.conn());
    }
    if (log.isDebugEnabled()) log.debug("关闭 Arrow 会话");
  }

  abstract void closeSession();
}
