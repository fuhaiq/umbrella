package com.umbrella.beanstalkd;

import java.util.concurrent.CompletableFuture;

public class BeanstalkdCommand extends CompletableFuture<String> {

	private final String request;

	public BeanstalkdCommand(String request) {
		this.request = request;
	}

	public String getRequest() {
		return request;
	}
}
