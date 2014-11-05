package com.umbrella.service.beanstalkd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeanstalkdKernelJob {
	
	public BeanstalkdKernelJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		Pattern pattern = Pattern.compile("(\\d+)");
		Matcher matcher = pattern.matcher(job);
		if (matcher.find()) {
			this.id = Long.parseLong(matcher.group(0));
		} else {
			throw new IllegalStateException("could not find job id");
		}
		pattern = Pattern.compile("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
		matcher = pattern.matcher(job);
		if (matcher.find()) {
			this.topicId = matcher.group(0);
		} else {
			throw new IllegalStateException("could not find topic id");
		}
	}

	public String getTopicId() {
		return topicId;
	}

	public long getId() {
		return id;
	}

	private long id;
	
	private String topicId;

	
}
