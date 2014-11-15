package com.umbrella.service.beanstalkd;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class BeanstalkdDBJob {
	
	public BeanstalkdDBJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		int firstSpace = job.indexOf(' ') + 1;
		int secondSpace = job.indexOf(' ', firstSpace);
		id = Integer.parseInt(StringUtils.substring(job, firstSpace, secondSpace));
		sqls = JSON.parseArray(StringUtils.substringBetween(job, "\r\n"));
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
