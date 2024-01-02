package org.umbrella.query;

import org.apache.arrow.vector.ipc.ArrowReader;

public interface EngineHandler {
  void dremio(String sql);

  void orc(String file);

  void orc(String file, String[] columns);

  void arrow(String file);

  void arrow(String file, String[] columns);

  void arrow(ArrowReader reader);

  void avro(String file);

  void avro(String file, String[] columns);
}
