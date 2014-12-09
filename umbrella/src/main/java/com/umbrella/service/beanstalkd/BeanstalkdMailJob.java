package com.umbrella.service.beanstalkd;

public class BeanstalkdMailJob {
	
	public BeanstalkdMailJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstIndex = job.indexOf(' ', 8) + 1;
		int secondIndex = job.indexOf(' ', firstIndex);
		id = Integer.parseInt(job.substring(firstIndex, secondIndex));
		firstIndex = job.indexOf("\r\n", secondIndex);
		email = job.substring(firstIndex + 2, job.length() - 2);
	}

	public long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	private final long id;

	private final String email;
}
