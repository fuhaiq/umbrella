package com.umbrella.service.beanstalkd;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class BeanstalkdSearchJob {

	public BeanstalkdSearchJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstSpace = job.indexOf(' ') + 1;
		int secondSpace = job.indexOf(' ', firstSpace);
		id = Integer.parseInt(StringUtils.substring(job, firstSpace, secondSpace));
		json = JSON.parseObject(StringUtils.substringBetween(job, "\r\n"));
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
