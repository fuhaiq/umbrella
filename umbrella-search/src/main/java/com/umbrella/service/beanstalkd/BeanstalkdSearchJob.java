package com.umbrella.service.beanstalkd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class BeanstalkdSearchJob {

	public BeanstalkdSearchJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		Pattern pattern = Pattern.compile("(\\d+)");
		Matcher matcher = pattern.matcher(job);
		if (matcher.find()) {
			id = Long.parseLong(matcher.group(0));
		} else {
			throw new IllegalStateException("could not find job id");
		}
		pattern = Pattern.compile("\\{.*\\}");
		matcher = pattern.matcher(job);
		if (matcher.find()) {
			json = JSON.parseObject(matcher.group(0));
		} else {
			throw new IllegalStateException("could not find json content");
		}
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
