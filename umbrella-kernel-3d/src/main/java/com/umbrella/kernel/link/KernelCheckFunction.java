package com.umbrella.kernel.link;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@Component
public class KernelCheckFunction implements Function<JSONArray, JSONArray> {

	@Autowired
	private KernelConfig config;

	@Override
	public JSONArray apply(JSONArray scripts) {
		for (int i = 0; i < scripts.size(); i++) {
			String script = scripts.getString(i);
			for (int badChar : config.getBadChar()) {
				if (script.indexOf(badChar) != -1) {
					JSONArray result = new JSONArray();
					JSONObject obj = new JSONObject();
					obj.put("index", i);
					obj.put("type", "error");
					obj.put("data", "Syntax::sntxf Bad character " + (char) badChar);
					result.add(obj);
					return result;
				}
			}
		}
		return null;
	}

}
