package com.umbrella.service.beanstalkd.kernel;

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

public class BeanstalkdTopicManager {
	
	@Inject private Session<Jedis> jedisSession;
	
	@Inject private SqlSessionManager manager;
	
	@Inject private Kernel kernel;
	
	private final String LOCK = "topic:lock:";
	
	@JedisCycle
	public boolean lockTopic(int topicId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(LOCK + topicId, "lock");
		if(response == 0) {
			return false;
		}
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", Status.EVALUATE.getValue());
		manager.update("topic.update", map);
		return true;
	}
	
	@KernelCycle
	public JSONObject evaluate(Elements scripts) throws SessionException, MathLinkException {
		Status status = Status.SUCCESS;
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate("/home/wesker/umbrella-openresty/www/static/temp/", scripts.get(i).text());
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
		JSONObject topicResult = new JSONObject();
		topicResult.put("status", status.getValue());
		topicResult.put("result", result);
		return topicResult;
	}
	
	public Elements getScriptElements(int topicId) throws SQLException {
		Map<String, String> topic = manager.selectOne("topic.select", topicId);
		String html = topic.get("html");
		if(Strings.isNullOrEmpty(html)) {
			throw new SQLException("no topic "+ topicId +" in db");
		}
		return Jsoup.parse(html).select("pre[class=\"mathematica hljs\"]");
	}
	
	@JedisCycle
	public void setResult(int topicId, JSONObject topicResult) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		String status = topicResult.getString("status");
		String result = topicResult.getJSONArray("result").toJSONString();
		//set topic's result
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", status);
		map.put("result", result);
		manager.update("topic.update", map);
		jedis.del(LOCK + topicId);
	}
	
	@JedisCycle
	public void resetTopicStatus(int topicId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", Status.WAITING.getValue());
		manager.update("topic.update", map);
		jedis.del(LOCK + topicId);
	}

	public enum Status {

		ABORT("-2"), SYNTAX_ERROR("-1"), WAITING("0"), EVALUATE("1"), SUCCESS("2");

		private String value;

		private Status(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}
}