package com.umbrella.service.kernel.action;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class JsonActionHandler extends SimpleChannelInboundHandler<JSONObject>{

	@Inject private Provider<Map<String, JsonAction>> actionMapping;
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JSONObject json) throws Exception {
		String id = checkNotNull(json.getString("id"), "action id is null");
		JsonAction action = checkNotNull(actionMapping.get().get(id), "action["+id+"] is not defined");
		action.act(ctx, json);
	}

}
