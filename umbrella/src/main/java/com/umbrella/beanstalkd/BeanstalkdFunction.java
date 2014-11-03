package com.umbrella.beanstalkd;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

@FunctionalInterface
public interface BeanstalkdFunction extends Function<String, BeanstalkdJob> {

	BeanstalkdFunction RESERVE = (job) -> {
		if(Strings.isNullOrEmpty(job)) return null;
		if (!job.startsWith("RESERVED")) throw new IllegalStateException("reserve job doesn't start with RESERVED");
		BeanstalkdJob beanstalkdJob = new BeanstalkdJob();
		Pattern pattern = Pattern.compile("(\\d+)");
		Matcher matcher = pattern.matcher(job);
		if (matcher.find()) {
			beanstalkdJob.setId(Long.parseLong(matcher.group(0)));
		} else {
			throw new IllegalStateException("could not find job id");
		}
		pattern = Pattern.compile("(\\D+\\R)");
		matcher = pattern.matcher(job);
		if (matcher.find()) {
			StringBuffer data = new StringBuffer(matcher.group(0));
			data.delete(0, 2);
			data.delete(data.length() - 2, data.length());
			beanstalkdJob.setData(data.toString());
		} else {
			throw new IllegalStateException("could not find job data");
		}
		return beanstalkdJob;
	};
}
