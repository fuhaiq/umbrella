package com.umbrella.service.netty.json;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.UUID;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;

public class KernelCommand implements JsonCommand {
	
	@Inject private Kernel kernel;

	@Override
	@KernelCycle
	public JSON exec(JSONObject in) throws Exception {
		String dir = checkNotNull(in.getString("dir"), "dir is null");
		JSONArray scripts = JSON.parseArray(checkNotNull(in.getString("scripts"), "scripts is null"));
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(scripts.getString(i));
			for(int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				obj.put("index", i);
				if(obj.getString("type").equals("error") || obj.getString("type").equals("abort")) {
					result.add(obj);
					break outer;
				}else if(obj.getString("type").equals("image")) {
					String uuid = UUID.randomUUID().toString() + ".gif";
					Files.write(obj.getBytes("data"), new File(dir + uuid));
					obj.replace("data", uuid);
				}
			}
			result.addAll(json);
			json.clear();
		}
		return result;
	}

}
