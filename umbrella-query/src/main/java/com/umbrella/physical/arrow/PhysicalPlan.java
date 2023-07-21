package com.umbrella.physical.arrow;

import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public interface PhysicalPlan {

    Schema schema();

    List<PhysicalPlan> children();

    VectorSchemaRoot execute();

    default String id() {
        return RandomStringUtils.randomAlphabetic(6);
    }
}
