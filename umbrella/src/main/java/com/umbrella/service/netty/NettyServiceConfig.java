package com.umbrella.service.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;

public class NettyServiceConfig {
	
	private String host;
	
	private int port;
	
	private NettyServiceType type;
	
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

	public NettyServiceType getType() {
		return type;
	}

	public void setType(String type) {
		if (type.equalsIgnoreCase("nio")) {
			this.type = new NettyServiceType.NIO();
		} else if (type.equalsIgnoreCase("epoll")) {
			this.type = new NettyServiceType.EPOLL();
		} else {
			throw new IllegalStateException("type must be one of NIO or EPOLL");
		}
	}
}
