package com.umbrella.service.beanstalkd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class BeanstalkdDBJob {
	
	public BeanstalkdDBJob(String job) {
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		Pattern pattern = Pattern.compile("(\\d+)");
		Matcher matcher = pattern.matcher(job);
		if (matcher.find()) {
			this.id = Long.parseLong(matcher.group(0));
		} else {
			throw new IllegalStateException("could not find job id");
		}
		pattern = Pattern.compile("\\[\\{.*\\}\\]");
		matcher = pattern.matcher(job);
		if (matcher.find()) {
			this.sqls = JSON.parseArray(matcher.group(0));
		} else {
			throw new IllegalStateException("could not find job data");
		}
	}

	private long id;
	
	private JSONArray sqls;

	public long getId() {
		return id;
	}

	public JSONArray getSqls() {
		return sqls;
	}
	
}
