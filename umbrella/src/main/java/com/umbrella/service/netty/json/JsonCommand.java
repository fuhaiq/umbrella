package com.umbrella.service.netty.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

@FunctionalInterface

public interface JsonCommand {

	JSON exec(JSONObject json) throws Exception;
	
}
