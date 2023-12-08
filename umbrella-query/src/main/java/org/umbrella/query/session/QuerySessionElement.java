package org.umbrella.query.session;

import org.apache.arrow.c.ArrowArrayStream;

import java.sql.Connection;
import java.util.Map;

record QuerySessionElement(Connection conn, Map<String, ArrowArrayStream> map) {
}
