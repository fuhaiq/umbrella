package com.umbrella.service;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

public class RpcServiceConfig {
	
	public RpcServiceConfig(String host, int port, RpcServiceType type) {
		this.host = host;
		this.port = port;
		this.type = type;
	}

	private String host;
	
	private int port;
	
	private RpcServiceType type;
	
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
	
	public EventLoopGroup getBoss() {
		return type.getBoss();
	}

	public EventLoopGroup getWorker() {
		return type.getWorker();
	}

	public Class<? extends ServerChannel> getChannelClass() {
		return type.getChannelClass();
	}
}
