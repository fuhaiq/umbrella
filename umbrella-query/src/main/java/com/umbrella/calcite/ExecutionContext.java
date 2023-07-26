package com.umbrella.calcite;

import org.apache.calcite.tools.RelBuilder;
import org.apache.commons.lang3.NotImplementedException;

import java.io.Closeable;
import java.io.IOException;

public class ExecutionContext implements Closeable  {

    public RelBuilder parquet(String uri) {
        throw new NotImplementedException("TODO");
    }

    @Override
    public void close() throws IOException {

    }
}
