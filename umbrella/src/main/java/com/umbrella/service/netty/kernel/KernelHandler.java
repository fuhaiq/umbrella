package com.umbrella.service.netty.kernel;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import static com.google.common.base.Preconditions.checkNotNull;

public class KernelHandler extends SimpleChannelInboundHandler<JSONObject> {

	@Inject private Kernel kernel;
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, JSONObject json) throws Exception {
		String dir = checkNotNull(json.getString("dir"), "dir is null");
		JSONArray scripts = JSON.parseArray(checkNotNull(json.getString("scripts"), "scripts is null"));
		ctx.writeAndFlush(evaluate(dir, scripts));
	}
	
	@KernelCycle
	public JSON evaluate(String dir, JSONArray scripts) throws Exception {
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(dir, scripts.getString(i));
			for(int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				obj.put("index", i);
			}
			result.addAll(json);
			for(int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				if(obj.getString("type").equals("error") || obj.getString("type").equals("abort")) {
					break outer;
				}
			}
			json.clear();
		}
		return result;
	}

}
