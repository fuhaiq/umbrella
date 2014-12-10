package com.umbrella.service.beanstalkd;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class BeanstalkdSearchJob {
	
	public BeanstalkdSearchJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstIndex = job.indexOf(' ', 8) + 1;
		int secondIndex = job.indexOf(' ', firstIndex);
		id = Integer.parseInt(job.substring(firstIndex, secondIndex));
		firstIndex = job.indexOf("\r\n", secondIndex);
		json = JSON.parseObject(job.substring(firstIndex + 2, job.length() - 2));
	}

	public long getId() {
		return id;
	}

	public JSONObject getJson() {
		return json;
	}

	private final long id;

	private final JSONObject json;
}
