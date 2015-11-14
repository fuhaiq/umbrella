package com.umbrella.kernel.link;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public interface Kernel {
	
	String ABORT = "<math>\n <mi>$Aborted</mi>\n</math>";
	
	String FAILED = "<math>\n <mi>$Failed</mi>\n</math>";
	
	String NULL = "<math>\n <mi>Null</mi>\n</math>";
	
	JSONArray evaluate(JSONObject json) throws Exception;

}