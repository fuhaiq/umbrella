package com.umbrella.service.beanstalkd;

import java.sql.SQLException;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

public class BeanstalkdKernelManager {
	
	private final Logger LOG = LogManager.getLogger(BeanstalkdKernelManager.class);

	@Inject private Session<Jedis> jedisSession;
	
	@Inject private SqlSessionManager manager;
	
	@Inject private Kernel kernel;
	
	private final String lock = "topic:kernel:lock:";
	
	@JedisCycle
	public boolean lockTopic(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(lock + topicId, "lock");
		if(response == 0) {
			return false;
		}
		LOG.info("锁定话题成功");
		Map<String, String> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", Status.EVALUATE.getValue());
		manager.update("topic.update", map);
		LOG.info("更新话题状态为计算中");
		jedis.del("topic:" + topicId);
		LOG.info("删除话题缓存");
		return true;
	}
	
	@KernelCycle
	public JSONObject evaluate(Elements scripts) throws SessionException, MathLinkException {
		Status status = Status.SUCCESS;
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(scripts.get(i).text());
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
	
	public Elements getScriptElements(String topicId) throws SQLException {
		String html = manager.selectOne("topic.selectHtml", topicId);
		if(Strings.isNullOrEmpty(html)) {
			throw new SQLException("no topic "+ topicId +" in db");
		}
		return Jsoup.parse(html).select("pre[class=brush:mathematica;toolbar:false]");
	}
	
	@JedisCycle
	public void setResult(String topicId, JSONObject topicResult) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		String status = topicResult.getString("status");
		String result = topicResult.getJSONArray("result").toJSONString();
		Map<String, String> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", status);
		map.put("result", result);
		manager.update("topic.update", map);
		LOG.info("更新话题状态,设置话题结果");
		jedis.del("topic:" + topicId);
		LOG.info("删除话题缓存");
		jedis.del(lock + topicId);
		LOG.info("释放话题锁");
	}
	
	@JedisCycle
	public void resetTopicStatus(String topicId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		Map<String, String> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", Status.WAITING.getValue());
		manager.update("topic.update", map);
		LOG.info("重置话题状态为等待");
		jedis.del(lock + topicId);
		LOG.info("释放话题锁");
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