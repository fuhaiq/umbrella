package com.umbrella.service;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RpcServiceType {
	
	public RpcServiceType(EventLoopGroup boss, EventLoopGroup worker, Class<? extends ServerChannel> channelClass) {
		this.boss = boss;
		this.worker = worker;
		this.channelClass = channelClass;
	}

	private EventLoopGroup boss;
	
	private EventLoopGroup worker;
	
	private Class<? extends ServerChannel> channelClass;
	
	public EventLoopGroup getBoss() {
		return boss;
	}

	public EventLoopGroup getWorker() {
		return worker;
	}

	public Class<? extends ServerChannel> getChannelClass() {
		return channelClass;
	}
	
	public static final class EPOLL extends RpcServiceType {

		public EPOLL() {
			super(new EpollEventLoopGroup(0x1, new ThreadFactoryBuilder().setNameFormat("epoll-boss-thread").build()),
					new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() + 0x1, new ThreadFactoryBuilder().setNameFormat("epoll-worker-thread").build()),
					EpollServerSocketChannel.class);
		}
		
	}
	
	public static final class NIO extends RpcServiceType {

		public NIO() {
			super(new NioEventLoopGroup(0x1, new ThreadFactoryBuilder().setNameFormat("nio-boss-thread").build()),
					new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() + 0x1, new ThreadFactoryBuilder().setNameFormat("nio-worker-thread").build()),
					NioServerSocketChannel.class);
		}
		
	}
}
