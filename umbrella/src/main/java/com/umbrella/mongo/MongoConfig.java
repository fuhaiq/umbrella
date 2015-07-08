package com.umbrella.mongo;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class MongoConfig extends GenericObjectPoolConfig {

	private int port;
	
	private String host;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
}
