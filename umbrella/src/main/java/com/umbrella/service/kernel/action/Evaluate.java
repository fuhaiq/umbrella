package com.umbrella.service.kernel.action;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.ChannelHandlerContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class Evaluate implements JsonAction{
	
	@Inject private Kernel kernel;

	@Override
	public void act(ChannelHandlerContext ctx, JSONObject json) throws Exception {
		String scriptsStr = checkNotNull(json.getString("scripts"), "input is null");
		JSONArray scripts = JSON.parseArray(scriptsStr);
		ctx.writeAndFlush(evaluate(scripts));
	}

	@KernelCycle
	public JSON evaluate(JSONArray scripts) throws SessionException, MathLinkException {
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(scripts.getString(i));
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
