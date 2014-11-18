package com.umbrella.service.beanstalkd;


public class BeanstalkdKernelJob {
	
	public BeanstalkdKernelJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstIndex = job.indexOf(' ', 8) + 1;
		int secondIndex = job.indexOf(' ', firstIndex);
		id = Integer.parseInt(job.substring(firstIndex, secondIndex));
		firstIndex = job.indexOf("\r\n", secondIndex);
		topicId = job.substring(firstIndex + 2, job.length() - 2);
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
