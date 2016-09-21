package com.umbrella.kernel.link.morse;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.pool2.ObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.umbrella.kernel.link.KernelConfig;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;

@Component
public class Morse {
	
	@Autowired
	private ObjectPool<KernelLink> kernelPool;

	@Autowired
	private ExecutorService service;
	
	@Autowired
	private KernelConfig config;
	
	@Autowired
	private MorseConfig morseConfig;
	
	public JSON decode(JSONObject in) throws Exception {
		JSONArray scripts = JSON.parseArray(checkNotNull(in.getString("scripts"), "[Invalid JSON]: scripts does not exsit"));
		for (int i = 0; i < scripts.size(); i++) {
			String script = scripts.getString(i);
			if(!Files.exists(Paths.get(morseConfig.getPath() + "/" + script + ".wav"))){
				JSONArray result = new JSONArray();
				JSONObject obj = new JSONObject();
				obj.put("index", i);
				obj.put("type", "error");
				obj.put("data", "Syntax::sntxf 找不到此摩尔斯电码文件 ");
				result.add(obj);
				return result;
			}
		}
		
		KernelLink kernelLink = kernelPool.borrowObject();
		CompletableFuture<JSON> future = new CompletableFuture<JSON>();
		service.execute(() -> {
			try {
				future.complete(morse(kernelLink, scripts, false));
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
	
	public JSON encode(JSONObject in) throws Exception {
		JSONArray scripts = JSON.parseArray(checkNotNull(in.getString("scripts"), "[Invalid JSON]: scripts does not exsit"));
		for (int i = 0; i < scripts.size(); i++) {
			String script = scripts.getString(i);
			for (char c : script.toCharArray()) {
				int cInt = (int) c;
				if (!morseConfig.getCodes().contains(cInt)) {
					JSONArray result = new JSONArray();
					JSONObject obj = new JSONObject();
					obj.put("index", i);
					obj.put("type", "error");
					obj.put("data", "Syntax::sntxf 错误字符 \"" + c + "\" 摩尔斯电码目前仅支持 英文字母 a-z A-Z 数字 0-9 空格 和字符 , . ! ? ");
					result.add(obj);
					return result;
				}
			}
		}
		KernelLink kernelLink = kernelPool.borrowObject();
		CompletableFuture<JSON> future = new CompletableFuture<JSON>();
		service.execute(() -> {
			try {
				future.complete(morse(kernelLink, scripts, true));
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
	
	private JSONArray morse(KernelLink kernelLink, JSONArray scripts, boolean encode) throws IOException, MathLinkException {
		JSONArray result = new JSONArray();
		outer: for (int i = 0; i < scripts.size(); i++) {
			JSONArray json;
			if(encode) {
				json = encode(kernelLink, scripts.getString(i));
			} else {
				json = decode(kernelLink, scripts.getString(i));
			}
			for (int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				obj.put("index", i);
				if (obj.getString("type").equals("error") || obj.getString("type").equals("abort")) {
					result.add(obj);
					break outer;
				}
			}
			result.addAll(json);
			json.clear();
		}
		return result;
	}
	
	private JSONArray encode(KernelLink kernelLink, String expression) throws MathLinkException {
		expression = expression.replace((char) 160, (char) 32);
		kernelLink.putFunction("EvaluatePacket", 1);
		kernelLink.putFunction("UmbrellaOther`encodeMorse", 1);
		kernelLink.put(expression);
		kernelLink.endPacket();
		kernelLink.flush();
		kernelLink.discardAnswer();
		return kernelLink.result();
	}
	
	private JSONArray decode(KernelLink kernelLink, String expression) throws MathLinkException {
		expression = expression.replace((char) 160, (char) 32);
		kernelLink.putFunction("EvaluatePacket", 1);
		kernelLink.putFunction("UmbrellaOther`decodeMorse", 1);
		kernelLink.put(expression);
		kernelLink.endPacket();
		kernelLink.flush();
		kernelLink.discardAnswer();
		return kernelLink.result();
	}

}
