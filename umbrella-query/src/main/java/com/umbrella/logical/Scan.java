package com.umbrella.logical;

import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

public record Scan(String uri, Schema schema, Optional<String[]> projection) implements LogicalPlan {
    @Override
    public Schema schema() {
        return schema;
    }

    @Override
    public List<LogicalPlan> children() {
        return List.of();
    }

    @Override
    public String toString() {
        return projection.isPresent() ?
                "Scan: " + uri + "; projection=" + StringUtils.join(projection.get().toString(), ",")
                :
                "Scan: " + uri + "; projection=None";
    }
}
