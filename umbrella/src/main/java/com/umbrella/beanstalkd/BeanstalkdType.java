package com.umbrella.beanstalkd;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class BeanstalkdType {
	
	public BeanstalkdType(EventLoopGroup group, Class<? extends SocketChannel> channelClass) {
		this.group = group;
		this.channelClass = channelClass;
	}
	
	public EventLoopGroup getGroup() {
		return group;
	}

	public Class<? extends SocketChannel> getChannelClass() {
		return channelClass;
	}

	private final EventLoopGroup group;
	
	private final Class<? extends SocketChannel> channelClass;
	
	public static final class NIO extends BeanstalkdType {

		public NIO() {
			super(new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 0x1, new ThreadFactoryBuilder().setNameFormat("beanstalkd-nio-thread").build()), NioSocketChannel.class);
		}
		
	}
	
	public static final class EPOLL extends BeanstalkdType {

		public EPOLL() {
			super(new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() + 0x1, new ThreadFactoryBuilder().setNameFormat("beanstalkd-epoll-thread").build()), EpollSocketChannel.class);
		}
		
	}
	
}
