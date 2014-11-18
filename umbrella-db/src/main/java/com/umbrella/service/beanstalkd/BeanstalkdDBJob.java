package com.umbrella.service.beanstalkd;

import com.alibaba.fastjson.JSONArray;

public class BeanstalkdDBJob {
	
	public BeanstalkdDBJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstIndex = job.indexOf(' ', 8) + 1;
		int secondIndex = job.indexOf(' ', firstIndex);
		id = Integer.parseInt(job.substring(firstIndex, secondIndex));
		firstIndex = job.indexOf("\r\n", secondIndex);
		sqls = JSONArray.parseArray(job.substring(firstIndex + 2, job.length() - 2));
	}

	private final long id;
	
	private final JSONArray sqls;

	public long getId() {
		return id;
	}

	public JSONArray getSqls() {
		return sqls;
	}
}
