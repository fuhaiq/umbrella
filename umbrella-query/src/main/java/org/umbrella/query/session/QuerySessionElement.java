package org.umbrella.query.session;

import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.jooq.lambda.tuple.Tuple2;

import java.sql.Connection;
import java.util.Map;

record QuerySessionElement(Connection conn, Map<String, Tuple2<ArrowArrayStream, ArrowReader>> map) {
}
