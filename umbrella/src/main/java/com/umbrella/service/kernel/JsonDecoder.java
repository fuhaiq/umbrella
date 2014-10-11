package com.umbrella.service.kernel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.nio.charset.Charset;
import java.util.List;

import com.alibaba.fastjson.JSON;

@Sharable
public class JsonDecoder extends MessageToMessageDecoder<ByteBuf>{
	
	private final Charset charset = Charset.forName("UTF-8");
	
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    	out.add(JSON.parseObject(msg.toString(charset)));
    }
}
