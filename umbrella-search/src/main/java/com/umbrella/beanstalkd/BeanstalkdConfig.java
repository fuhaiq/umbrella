package com.umbrella.beanstalkd;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class BeanstalkdConfig extends GenericObjectPoolConfig {

	private BeanstalkdType type;

	private int port;

	private String host;

	private int timeout;

	public void setType(String type) {
		if (type.equalsIgnoreCase("nio")) {
			this.type = new BeanstalkdType.NIO();
		} else if (type.equalsIgnoreCase("epoll")) {
			this.type = new BeanstalkdType.EPOLL();
		} else {
			throw new IllegalStateException("type must be one of NIO or EPOLL");
		}
	}

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

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public EventLoopGroup getGroup() {
		return type.getGroup();
	}

	public Class<? extends SocketChannel> getChannelClass() {
		return type.getChannelClass();
	}

}
