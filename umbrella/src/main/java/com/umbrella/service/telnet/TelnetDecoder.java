package com.umbrella.service.telnet;

import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;

public class TelnetDecoder extends DelimiterBasedFrameDecoder{
	public TelnetDecoder() {
		super(8192, Delimiters.lineDelimiter());
	}
}
