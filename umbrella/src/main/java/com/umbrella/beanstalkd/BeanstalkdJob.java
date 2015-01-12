package com.umbrella.beanstalkd;

public class BeanstalkdJob {
	
	public BeanstalkdJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstIndex = job.indexOf(' ', 8) + 1;
		int secondIndex = job.indexOf(' ', firstIndex);
		id = Integer.parseInt(job.substring(firstIndex, secondIndex));
		firstIndex = job.indexOf("\r\n", secondIndex);
		data = job.substring(firstIndex + 2, job.length() - 2);
	}
	
	private final long id;
	
	private final String data;

	public long getId() {
		return id;
	}

	public String getData() {
		return data;
	}
}
