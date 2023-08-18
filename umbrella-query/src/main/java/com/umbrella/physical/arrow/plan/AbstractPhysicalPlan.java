package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.VectorBatch;

import java.util.List;

public abstract class AbstractPhysicalPlan implements PhysicalPlan {
    protected final PhysicalPlan input;
    protected AbstractPhysicalPlan(PhysicalPlan input) {
        this.input = input;
    }
    @Override
    public List<PhysicalPlan> getInputs() {
        return List.of(input);
    }
    @Override
    public VectorBatch execute() {
        try(var output = input.execute()) {
            return execute(output);
        }
    }
    protected abstract VectorBatch execute(VectorBatch input);
}
