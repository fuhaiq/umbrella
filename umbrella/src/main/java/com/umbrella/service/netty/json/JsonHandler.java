package com.umbrella.service.netty.json;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class JsonHandler extends SimpleChannelInboundHandler<JSONObject> {

	@Inject
	Provider<Map<String, JsonCommand>> commands;
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JSONObject json) throws Exception {
		String id = checkNotNull(json.getString("id"), "command's id is null");
		JsonCommand command = checkNotNull(commands.get().get(id), "no command match with " + id);
		ctx.writeAndFlush(command.exec(json));
	}

}
