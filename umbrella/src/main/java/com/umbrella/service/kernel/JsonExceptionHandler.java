package com.umbrella.service.kernel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

import com.alibaba.fastjson.JSONObject;

@Sharable
public class JsonExceptionHandler extends SimpleChannelInboundHandler<JSONObject>{

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JSONObject json) throws Exception {}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		JSONObject exception = new JSONObject();
		exception.put("id", "exception");
		exception.put("msg", cause.getMessage());
		ctx.writeAndFlush(exception);
	}

}
