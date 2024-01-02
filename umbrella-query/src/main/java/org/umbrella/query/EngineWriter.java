package org.umbrella.query;

import org.jooq.DSLContext;

public interface EngineWriter {
  DSLContext mysql();
}
