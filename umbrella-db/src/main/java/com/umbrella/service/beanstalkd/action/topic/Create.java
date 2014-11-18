package com.umbrella.service.beanstalkd.action.topic;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionManager;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.umbrella.redis.JedisCycle;
import com.umbrella.service.beanstalkd.action.DBAction;
import com.umbrella.session.Session;

public class Create implements DBAction {
	
	@Inject private SqlSessionManager manager;
	
	@Inject private Session<Jedis> jedisSession;

	@Override
	@JedisCycle
	public void accept(JSONObject json) {
		String topicId = checkNotNull(json.getString("key"), "topic key is empty");
		String updated = checkNotNull(json.getString("updated"), "topic updated is empty");
		Jedis jedis = jedisSession.get();
		if(!jedis.exists("topic:" + topicId)) throw new IllegalStateException("topic "+ topicId +" is not exist");
		List<String> values = jedis.hmget("topic:" + topicId, "title", "html");
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("title", values.get(0));
		map.put("html", values.get(1));
		map.put("created", updated);
		map.put("status", 0);
		manager.insert("topic.insert", map);
	}

}
