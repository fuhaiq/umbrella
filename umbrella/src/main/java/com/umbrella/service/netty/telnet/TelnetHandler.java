package com.umbrella.service.netty.telnet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.socket.emitter.Emitter;
import redis.clients.jedis.Jedis;

public class TelnetHandler extends SimpleChannelInboundHandler<String>{
	
	private final Logger LOG = LogManager.getLogger("TelnetHandler");
	
	@Inject private ServiceManager manager;
	
	@Inject private Session<Jedis> jedisSession;
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		if(msg.equalsIgnoreCase("info")) {
			ctx.writeAndFlush(manager.toString() + "\r\n");
		} else if(msg.equalsIgnoreCase("exit")) {
			ctx.channel().close();
		} else if(msg.equalsIgnoreCase("shutdown")) {
			ctx.writeAndFlush("shutdown all services\r\n").addListener(r->manager.stopAsync());
		} else if(msg.equalsIgnoreCase("test")) {
			emit();
			ctx.writeAndFlush("done\r\n");
		} else {
			ctx.writeAndFlush("no command "+msg+" defined\r\n");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOG.error(cause.getMessage());
		ctx.writeAndFlush("exception in server side "+cause.getMessage()+"\r\n");
	}
	
	@JedisCycle
	public void emit() throws Exception {
		Jedis jedis = jedisSession.get();
		Emitter emitter = new Emitter(jedis);
		JSONObject json = new JSONObject();
		json.put("pid", 23);
		json.put("tid", 55);
		emitter.to("umbrella_room").emit("event:umbrella", json);
	}
}
