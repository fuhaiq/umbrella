package org.umbrella.query.session;

import lombok.RequiredArgsConstructor;
import org.apache.arrow.c.ArrowArrayStream;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.jooq.lambda.tuple.Tuple2;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class QuerySessionElement {
    public final Connection conn;
    public final Map<String, Tuple2<ArrowArrayStream, ArrowReader>> map = new HashMap<>();
}
