package com.umbrella.service.beanstalkd;

import java.util.List;
import java.util.Map;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.beanstalkd.BeanstalkdCycle;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class BeanstalkdKernelManager {
	
	private final Logger LOG = LogManager.getLogger(BeanstalkdKernelManager.class);

	@Inject private Session<Jedis> jedisSession;
	
	@Inject private Session<Beanstalkd> beanSession;
	
	@Inject private Kernel kernel;
	
	@Inject private ObjectPool<Jedis> pool;
	
	@JedisCycle
	public String setToEvaluateThenGetUpdated(String topicId) throws SessionException {
		String result = null;
		Jedis jedis = jedisSession.get();
		jedis.watch("topic:" + topicId);
		Transaction tx = jedis.multi();
		Jedis assist = null;
		try{
			assist = pool.borrowObject();
			CAS : while(true) {
				if(!assist.exists("topic:" + topicId)) {
					LOG.info("话题不存在!直接退出");
					tx.discard();
					jedis.unwatch();
					break CAS;
				}
				List<String> statusAndUpdated = assist.hmget("topic:" + topicId, "status", "updated");
				String status = statusAndUpdated.get(0);
				String updated = statusAndUpdated.get(1);
				if (!status.equals(Status.WAITING.getValue())) {
					LOG.info("话题状态不是等待，直接退出");
					tx.discard();
					jedis.unwatch();
					break CAS;
				}
				tx.hset("topic:" + topicId, "status", Status.EVALUATE.getValue());
				List<?> txResult = tx.exec();
				if (txResult == null || txResult.size() == 0) {
					LOG.info("锁话题失败，重新进入判断");
					jedis.watch("topic:" + topicId);
					tx = jedis.multi();
					continue CAS;
				}
				LOG.info("锁话题成功");
				result = updated == null ? "" : updated;
				break CAS;
			}
		} catch(Exception e) {
			throw new SessionException(e);
		} finally {
			try {
				pool.returnObject(assist);
			} catch (Exception e) {
				throw new SessionException(e);
			}
		}
		return result;
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
	
	@JedisCycle
	public Elements getScriptElements(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		if(!jedis.exists("topic:" + topicId)) return null;
		String html = jedis.hget("topic:" + topicId, "html");
		return Jsoup.parse(html).select("pre[class=brush:mathematica;toolbar:false]");
	}
	
	@JedisCycle
	@BeanstalkdCycle
	public void setResult(String topicId, JSONObject topicResult, String updated) throws SessionException {
		Jedis jedis = jedisSession.get();
		jedis.watch("topic:" + topicId);
		Transaction tx = jedis.multi();
		Jedis assist = null;
		try {
			assist = pool.borrowObject();
			if(!assist.exists("topic:" + topicId)) {
				LOG.info("话题不存在!直接退出");
				tx.discard();
				jedis.unwatch();
				return;
			}
			String latestUpdated = assist.hget("topic:" + topicId, "updated");
			latestUpdated = latestUpdated == null ? "" : latestUpdated;
			if(!updated.equals(latestUpdated)) {
				LOG.info("话题时间戳不对应!直接退出");
				tx.discard();
				jedis.unwatch();
				return;
			}
			Map<String, String> map = Maps.newHashMap();
			String status = topicResult.getString("status");
			String result = topicResult.getJSONArray("result").toJSONString();
			map.put("status", status);
			map.put("result", result);
			tx.hmset("topic:" + topicId, map);
			List<?> txResult = tx.exec();
			if (txResult == null || txResult.size() == 0) {
				LOG.info("设置话题结果失败，可能话题被修改");
			} else {
				LOG.info("设置话题结果成功,分发DB任务");
				Beanstalkd bean = beanSession.get();
				JSONArray sqls = new JSONArray();
				JSONObject sql = new JSONObject();
				sql.put("id", "topic.add.result");
				sql.put("key", topicId);
				sqls.add(sql);
				bean.use("db");
				bean.put(sqls.toJSONString());
			}
		} catch (Exception e) {
			throw new SessionException(e);
		} finally {
			try {
				pool.returnObject(assist);
			} catch (Exception e) {
				throw new SessionException(e);
			}
		}
		
	}
	
	@JedisCycle
	public void resetTopicStatus(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		jedis.hset("topic:" + topicId, "status", Status.WAITING.getValue());
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