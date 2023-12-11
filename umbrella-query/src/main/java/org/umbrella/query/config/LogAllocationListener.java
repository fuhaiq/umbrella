package org.umbrella.query.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.arrow.memory.AllocationListener;
import org.apache.arrow.memory.AllocationOutcome;
import org.apache.arrow.memory.BufferAllocator;

@Slf4j
public class LogAllocationListener implements AllocationListener {
    @Override
    public void onPreAllocation(long size) {
        if(log.isTraceEnabled()){
            log.trace(String.format("预申请堆外内存 %d bytes", size));
        }
    }

    @Override
    public void onAllocation(long size) {
        if(log.isTraceEnabled()) {
            log.trace(String.format("申请堆外内存 %d bytes", size));
        }
    }

    @Override
    public void onRelease(long size) {
        if(log.isTraceEnabled()) {
            log.trace(String.format("释放堆外内存 %d bytes", size));
        }
    }

    @Override
    public boolean onFailedAllocation(long size, AllocationOutcome outcome) {
        return AllocationListener.super.onFailedAllocation(size, outcome);
    }

    @Override
    public void onChildAdded(BufferAllocator parentAllocator, BufferAllocator childAllocator) {
        AllocationListener.super.onChildAdded(parentAllocator, childAllocator);
    }

    @Override
    public void onChildRemoved(BufferAllocator parentAllocator, BufferAllocator childAllocator) {
        AllocationListener.super.onChildRemoved(parentAllocator, childAllocator);
    }
}
