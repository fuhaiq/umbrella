package org.umbrella.query;

import org.jooq.DSLContext;

public interface EngineReader {
  DSLContext duckdb();

  DSLContext dremio();
}
