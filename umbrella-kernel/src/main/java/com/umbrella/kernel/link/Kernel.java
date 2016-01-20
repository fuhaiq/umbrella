package com.umbrella.kernel.link;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.apache.commons.pool2.ObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Files;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;

@Component
public class Kernel {

	@Autowired
	private ObjectPool<KernelLink> kernelPool;

	@Autowired
	private ExecutorService service;
	
	@Autowired
	private Function<JSONArray, JSONArray> check;

	@Autowired
	private KernelConfig config;
	
	public JSONArray evaluate(JSONObject in) throws Exception {
		String dir = checkNotNull(in.getString("dir"), "参数错误: 图片目录不存在");
		JSONArray scripts = JSON.parseArray(checkNotNull(in.getString("scripts"), "参数错误: 计算脚本不存在"));
		JSONArray err = check.apply(scripts);
		if(null != err) return err;
		KernelLink kernelLink = kernelPool.borrowObject();
		CompletableFuture<JSONArray> future = new CompletableFuture<JSONArray>();
		service.execute(() -> {
			try {
				future.complete(evaluate_inner(kernelLink, dir, scripts));
			} catch (MathLinkException e) {
				kernelLink.clearError();
				kernelLink.newPacket();
				future.completeExceptionally(e);
			} catch (Exception e) {
				future.completeExceptionally(e);
			} finally {
				if (kernelLink != null) {
					try {
						kernelPool.returnObject(kernelLink);
					} catch (Exception e) {
						future.completeExceptionally(e);
					}
				}
			}
		});
		try {
			return future.get(config.getTimeConstrained(), TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			kernelLink.abandonEvaluation();
			throw e;
		}
	}

	private JSONArray evaluate_inner(KernelLink kernelLink, String dir, JSONArray scripts) throws IOException, MathLinkException {
		JSONArray result = new JSONArray();
		outer: for (int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel(kernelLink, scripts.getString(i));
			for (int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				obj.put("index", i);
				if (obj.getString("type").equals("error") || obj.getString("type").equals("abort")) {
					result.add(obj);
					break outer;
				} else if (obj.getString("type").equals("image")) {
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

	private JSONArray kernel(KernelLink kernelLink, String expression) throws MathLinkException {
		expression = expression.replace((char) 160, (char) 32);
		kernelLink.putFunction("EnterTextPacket", 1);
		kernelLink.put(expression);
		kernelLink.endPacket();
		kernelLink.flush();
		kernelLink.discardAnswer();
		return kernelLink.result();
	}

}
