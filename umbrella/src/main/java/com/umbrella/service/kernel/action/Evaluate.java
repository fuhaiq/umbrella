package com.umbrella.service.kernel.action;

import static com.google.common.base.Preconditions.checkNotNull;
import io.netty.channel.ChannelHandlerContext;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelTransaction;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class Evaluate implements JsonAction{
	
	@Inject private Kernel kernel;

	@Override
	public void act(ChannelHandlerContext ctx, JSONObject json) throws Exception {
		String input = checkNotNull(json.getString("input"), "input is null");
		ctx.writeAndFlush(evaluate(input));
	}

	@KernelTransaction
	public JSONArray evaluate(String input) throws SessionException, MathLinkException {
		return kernel.evaluate(input);
	}
}
