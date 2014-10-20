package com.umbrella.beanstalkd;

import static com.google.common.base.Preconditions.checkState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Queues;

public class BeanstalkdConnection extends SimpleChannelInboundHandler<String>{
	
	private Queue<BeanstalkdCommand> queue = Queues.newConcurrentLinkedQueue();
	
	private Channel channel;
	
	private AtomicReference<ConnectionState> state = new AtomicReference<ConnectionState>(ConnectionState.Ready);
	
	public void close() {
		if(channel != null) channel.close();
	}
	
	public boolean isConnected() {
		while(true) {
			ConnectionState current = state.get();
			switch(current) {
				case Closed:
					return false;
				case Connected:
					return channel.isActive();
				case Ready:
					continue;
				default:
					throw new IllegalStateException("no value of " + current);
				}
		}
	}
	
	public String emit(String request) throws InterruptedException, ExecutionException {
		while(true) {
			ConnectionState current = state.get();
			switch(current) {
				case Closed:
					throw new IllegalStateException("channel is in CLOSED state");
				case Connected:
					checkState(channel != null && channel.isActive(), "channel is in CONNECTED state, but it's null or inactive");
					BeanstalkdCommand command = new BeanstalkdCommand(request);
					queue.add(command);
					channel.writeAndFlush(command.getRequest());
					return command.get();
				case Ready:
					continue;
				default:
					throw new IllegalStateException("no value of " + current);
			}
		}
	}
	
	public String emit(String request, int timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		while(true) {
			ConnectionState current = state.get();
			switch(current) {
				case Closed:
					throw new IllegalStateException("channel is in CLOSED state");
				case Connected:
					checkState(channel != null && channel.isActive(), "channel is in CONNECTED state, but it's null or inactive");
					BeanstalkdCommand command = new BeanstalkdCommand(request);
					queue.add(command);
					channel.writeAndFlush(command.getRequest());
					return command.get(timeout, unit);
				case Ready:
					continue;
				default:
					throw new IllegalStateException("no value of " + current);
			}
		}
    }
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		channel = ctx.channel();
		state.set(ConnectionState.Connected);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		state.set(ConnectionState.Closed);
		queue.stream().forEach(r->{
			if(!r.isDone()) {
				if(r.getRequest().equals("reserve\r\n")) {
					r.complete("");
				} else {
					r.completeExceptionally(new Exception("connection closed exceptionally"));
				}
			}
		});
		queue.clear();
		queue = null;
		channel = null;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		while(!queue.isEmpty()) {
			BeanstalkdCommand command = queue.remove();
			command.complete(msg);
			break;
		}
	}

	public enum ConnectionState {
		Ready, Connected, Closed
	}
}
