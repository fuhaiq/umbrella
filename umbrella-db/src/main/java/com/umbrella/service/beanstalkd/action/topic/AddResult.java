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

public class AddResult implements DBAction {

	@Inject private SqlSessionManager manager;
	
	@Inject private Session<Jedis> jedisSession;
	
	@Override
	@JedisCycle
	public void accept(JSONObject json) {
		String topicId = checkNotNull(json.getString("key"), "topic key is empty");
		Jedis jedis = jedisSession.get();
		if(!jedis.exists("topic:" + topicId)) throw new IllegalStateException("topic "+ topicId +" is not exist");
		List<String> values = jedis.hmget("topic:" + topicId, "status", "result");
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", values.get(0));
		map.put("result", values.get(1));
		manager.insert("topic.update", map);
	}

}
