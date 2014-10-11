package com.umbrella.kernel;

import com.alibaba.fastjson.JSONArray;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public interface Kernel {
	
	String ABORT = "<math>\n <mi>$Aborted</mi>\n</math>";
	
	JSONArray evaluate(String expression) throws MathLinkException, SessionException;

}