package com.topfine.recommend.config;

import redis.clients.jedis.JedisPoolConfig;

public class RedisConfig extends JedisPoolConfig {
	
	private String host;
	
	private int port;
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
