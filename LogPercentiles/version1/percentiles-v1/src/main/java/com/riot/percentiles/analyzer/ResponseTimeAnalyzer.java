package com.riot.percentiles.analyzer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.riot.percentiles.pojo.AccessLog;
import com.riot.percentiles.utils.HttpMethod;

@Component
public class ResponseTimeAnalyzer {

	private final Log logger = LogFactory.getLog(ResponseTimeAnalyzer.class);

	@Autowired
	private List<AccessLog> dataSet;

	private final List<Double> percents = Lists.newArrayList(0.9d, 0.95d, 0.99d);

	/**
	 * calculates the 90%, 95% and 99% percentile response time for READ API requests</br>
	 * time-complexity = O(N*logN)</br>
	 * space-complexity = O(N)</br>
	 * <i>N refers to the total size of all logs under target folder</i>
	 */
	@PostConstruct
	public void analyse() {
		List<AccessLog> filteredAndSorted = dataSet.stream()
				.filter(log -> log.getHttpMethod() == HttpMethod.GET)
				.sorted(Comparator.comparing(AccessLog::getResponseTime)).collect(Collectors.toList());
		percents.forEach(percent -> {
			int position = new Double((percent * filteredAndSorted.size()) - 1).intValue();
			int responseTime = filteredAndSorted.get(position).getResponseTime();
			logger.info(percent * 100 + "% of requests return a response in " + responseTime + " ms ");
		});
	}

}
