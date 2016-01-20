package com.umbrella.kernel.link;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;

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
					obj.put("type", "error");
					obj.put("data", "Syntax::sntxf Bad character " + (char) badChar);
					result.add(obj);
					return result;
				}
			}
			String errMsg = hadBadEscape(0, script);
			if (!Strings.isNullOrEmpty(errMsg)) {
				JSONArray result = new JSONArray();
				JSONObject obj = new JSONObject();
				obj.put("type", "error");
				obj.put("data", "Syntax::sntxf " + errMsg);
				result.add(obj);
				return result;
			}
		}
		return null;
	}

	private String hadBadEscape(int start, final String expression) {
		int slash = expression.indexOf(92, start);
		if (slash != -1) {
			int leftSquareBrackets = slash + 1;
			if (expression.charAt(leftSquareBrackets) != 91)
				return "\\ must be followed by [";
			int rightSquareBrackets = expression.indexOf(93, leftSquareBrackets + 1);
			if (rightSquareBrackets == -1)
				return "there is no ] corresponding to [";
			String escape = expression.substring(leftSquareBrackets + 1, rightSquareBrackets);
			if (Strings.isNullOrEmpty(escape))
				return "there is no escape character";
			if (!config.getEscapes().contains(escape))
				return "bad escape : " + escape;
			return hadBadEscape(rightSquareBrackets + 1, expression);
		}
		return null;
	}

}
