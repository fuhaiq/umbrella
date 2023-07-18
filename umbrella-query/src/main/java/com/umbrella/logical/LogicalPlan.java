package com.umbrella.logical;


import java.util.List;

import org.apache.arrow.vector.types.pojo.Schema;
import org.apache.commons.lang3.RandomStringUtils;


public interface LogicalPlan {

    Schema schema();

    List<LogicalPlan> children();

    default String id() {
        return RandomStringUtils.randomAlphabetic(6);
    }

}
