package com.riot.percentiles.partitioner;

import java.util.List;
import java.util.stream.Collectors;

import com.riot.percentiles.pojo.AccessLog;

public class RangePartitioner {
	
	/**
	 * construct range partitioner using sample list, then sort sample list by acs
	 * order.
	 * 
	 * @param list of integer
	 */
	public RangePartitioner(List<Integer> samples) {
		this.samples = samples.stream().sorted().collect(Collectors.toList());
	}

	private final List<Integer> samples;

	/**
	 * get number of partitions
	 * @return number of partitions
	 */
	public int numPartitions() {
		return samples.size();
	}
	
	/**
	 * @param access log
	 * @return partition index for a given access log
	 */
	public int getPartition(AccessLog log) {
		int index = 0;
		for (; index < samples.size(); index++) {
			if (log.getResponseTime() <= samples.get(index)) {
				return index;
			}
		}
		return index;
	}

}
