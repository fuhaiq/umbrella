package com.umbrella.service.beanstalkd;

import org.apache.commons.lang3.StringUtils;

public class BeanstalkdKernelJob {
	
	public BeanstalkdKernelJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstSpace = job.indexOf(' ') + 1;
		int secondSpace = job.indexOf(' ', firstSpace);
		id = Integer.parseInt(StringUtils.substring(job, firstSpace, secondSpace));
		topicId = StringUtils.substringBetween(job, "\r\n");
	}

	public String getTopicId() {
		return topicId;
	}

	public long getId() {
		return id;
	}

	private final long id;
	
	private final String topicId;

	
}
