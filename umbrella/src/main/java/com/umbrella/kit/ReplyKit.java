package com.umbrella.kit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionManager;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class ReplyKit {

	@Inject private Session<Jedis> jedisSession;

	@Inject private SqlSessionManager manager;

	@Inject private Kernel kernel;

	private final String LOCK = "reply:lock:";

	private final static String PATH = "/home/wesker/umbrella-openresty/www/static/reply/";
	
	@JedisCycle
	public boolean lockReply(int replyId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(LOCK + replyId, "lock");
		if(response == 0) {
			return false;
		}
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", replyId);
		map.put("status", Status.EVALUATE.getValue());
		manager.update("reply.update", map);
		return true;
	}
	
	@KernelCycle
	public JSONObject evaluate(int replyId, Elements scripts) throws SessionException, MathLinkException, IOException {
		String replyPath = PATH + replyId + "/";
		Path path = Paths.get(replyPath);
		Files.deleteIfExists(path);
		Files.createDirectory(path);
		Status status = Status.SUCCESS;
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(replyPath, scripts.get(i).text());
			for(int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				obj.put("index", i);
			}
			result.addAll(json);
			for(int j = 0; j < json.size(); j++) {
				JSONObject obj = json.getJSONObject(j);
				if(obj.getString("type").equals("error")) {
					status = Status.SYNTAX_ERROR;
					break outer;
				} else if (obj.getString("type").equals("abort")) {
					status = Status.ABORT;
					break outer;
				}
			}
			json.clear();
		}
		JSONObject replyResult = new JSONObject();
		replyResult.put("status", status.getValue());
		replyResult.put("result", result);
		return replyResult;
	}
	
	public Elements getScriptElements(int replyId) throws SQLException {
		Map<String, String> reply = manager.selectOne("reply.select", replyId);
		if(reply == null) {
			throw new SQLException("reply is null");
		}
		String html = reply.get("html");
		if(reply.get("status") ==  Status.NO_CODE.getValue() || Strings.isNullOrEmpty(html)) {
			throw new SQLException("no mathematica code[status=3] or html is null");
		}
		return Jsoup.parse(html).select("pre[class=\"mathematica hljs\"]");
	}
	
	@JedisCycle
	public void setResult(int replyId, JSONObject replyResult) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		String status = replyResult.getString("status");
		String result = replyResult.getJSONArray("result").toJSONString();
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", replyId);
		map.put("status", status);
		map.put("result", result);
		manager.update("reply.update", map);
		jedis.del(LOCK + replyId);
	}
	
	@JedisCycle
	public void resetReplyStatus(int replyId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", replyId);
		map.put("status", Status.WAITING.getValue());
		manager.update("reply.update", map);
		jedis.del(LOCK + replyId);
	}

	public enum Status {

		ABORT("-2"), SYNTAX_ERROR("-1"), WAITING("0"), EVALUATE("1"), SUCCESS(
				"2"), NO_CODE("3");

		private String value;

		private Status(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

}
