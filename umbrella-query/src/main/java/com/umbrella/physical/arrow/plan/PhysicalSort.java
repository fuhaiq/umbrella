package com.umbrella.physical.arrow.plan;

import com.umbrella.physical.arrow.VectorBatch;

public class PhysicalSort extends AbstractPhysicalPlan {
    private final int offset;
    private final int fetch;

    public PhysicalSort(PhysicalPlan input, int offset, int fetch) {
        super(input);
        this.offset = offset;
        this.fetch = fetch;
    }


    @Override
    protected VectorBatch execute(VectorBatch input) {
        return input.slice(offset, fetch);
    }

    @Override
    public String toString() {
        return "PhysicalSort{" +
                "offset=" + offset +
                ", fetch=" + fetch +
                '}';
    }
}
