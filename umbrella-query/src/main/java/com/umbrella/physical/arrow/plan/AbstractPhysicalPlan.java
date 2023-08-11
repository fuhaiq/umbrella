package com.umbrella.physical.arrow.plan;

import org.apache.arrow.vector.VectorSchemaRoot;

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
    public VectorSchemaRoot execute() {
        try(var output = input.execute()) {
            var ret = execute(output);
            if(!ret.syncSchema()) throw new RuntimeException("Failed synchronizes the schema from the current vectors");
            return ret;
        }
    }
    protected abstract VectorSchemaRoot execute(VectorSchemaRoot input);
}
