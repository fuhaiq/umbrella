package com.umbrella.service.netty.json;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import com.alibaba.fastjson.JSON;

@Sharable
public class JsonEncoder extends MessageToMessageEncoder<JSON>{

	@Override
	protected void encode(ChannelHandlerContext ctx, JSON msg, List<Object> out) throws Exception {
		out.add(wrappedBuffer((msg.toJSONString()+"\n").getBytes()));
	}

}
