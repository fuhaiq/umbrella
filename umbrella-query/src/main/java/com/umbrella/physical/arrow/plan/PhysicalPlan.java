package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.VectorBatch;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

public interface PhysicalPlan {
    List<PhysicalPlan> getInputs();
    default String explain() {
        return format(this, 0);
    }

    default String format(PhysicalPlan plan, int indent) {
        var b = new StringBuilder();
        b.append("  ".repeat(Math.max(0, indent)));
        b.append(plan.toString()).append("\n");
        plan.getInputs().forEach(it -> b.append(format(it, indent + 1)));
        return b.toString();
    }
    VectorBatch execute();
    default String id() {
        return RandomStringUtils.randomAlphabetic(6);
    }
}
