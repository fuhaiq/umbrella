package com.umbrella.redis;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class JedisConfig extends GenericObjectPoolConfig {

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
