package com.umbrella.beanstalkd;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class BeanstalkdConfig extends GenericObjectPoolConfig{

	private int port;
	
	private String host;
	
	private int timeout;

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
		return group;
	}

	public Class<? extends SocketChannel> getChannelClass() {
		return channelClass;
	}

	private final EventLoopGroup group = new NioEventLoopGroup();
	
	private final Class<? extends SocketChannel> channelClass = NioSocketChannel.class;
}
