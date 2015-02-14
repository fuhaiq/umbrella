package com.umbrella.service.netty.json;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;

import com.alibaba.fastjson.JSONObject;

@Sharable
public class JsonExceptionHandler extends SimpleChannelInboundHandler<JSONObject>{
	
	private final Logger LOG = LogManager.getLogger("exception");

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JSONObject json) throws Exception {}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(!"Timeout waiting for idle object".equals(cause.getMessage())
				&& !"MathLink connection was lost.".equals(cause.getMessage())) {
			LOG.error(cause.getMessage(), cause);
		}
		JSONObject exception = new JSONObject();
		exception.put("id", "exception");
		exception.put("msg", cause.getMessage());
		ctx.writeAndFlush(exception);
	}

}
