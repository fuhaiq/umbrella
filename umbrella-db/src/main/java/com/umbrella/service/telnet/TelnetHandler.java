package com.umbrella.service.telnet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;

public class TelnetHandler extends SimpleChannelInboundHandler<String>{
	
	private final Logger LOG = LogManager.getLogger(TelnetHandler.class);
	
	@Inject private ServiceManager manager;
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		if(msg.equalsIgnoreCase("info")) {
			ctx.writeAndFlush(manager.toString() + "\r\n");
		} else if(msg.equalsIgnoreCase("exit")) {
			ctx.channel().close();
		} else if(msg.equalsIgnoreCase("shutdown")) {
			ctx.writeAndFlush("shutdown all services\r\n").addListener(r->manager.stopAsync());
		} else {
			ctx.writeAndFlush("no command "+msg+" defined\r\n");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.error(cause.getMessage());
		ctx.writeAndFlush("exception in server side "+cause.getMessage()+"\r\n");
	}
}
