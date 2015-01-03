package com.umbrella.kernel;

import com.alibaba.fastjson.JSONArray;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public interface Kernel {
	
	String ABORT = "$Aborted";
	
	String FAILED = "<math>\n <mi>$Failed</mi>\n</math>";
	
	String NULL = "<math>\n <mi>Null</mi>\n</math>";
	
	JSONArray evaluate(String dir, String expression) throws MathLinkException, SessionException;

}