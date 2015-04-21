package com.umbrella.kit;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import org.apache.ibatis.session.SqlSessionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class TopicKit {
	
	@Inject private Session<Jedis> jedisSession;
	
	@Inject private SqlSessionManager manager;
	
	@Inject private Kernel kernel;
	
	private final String LOCK = "topic:lock:";
	
	private final static String PATH = "/home/wesker/umbrella-openresty/www/static/topic/";
	
	private final Logger LOG = LogManager.getLogger("topickit");
	
	public void create(JSONObject topic) throws SessionException, SQLException, MathLinkException, IOException {
		if(Objects.isNull(topic)) {
			throw new SQLException("topic is null");
		}
		int status = topic.getIntValue("status");
		if(status != Status.WAITING.value) {
			throw new SQLException("bad topic status["+ status +"], expect " + Status.WAITING.getValue());
		}
		int topicId = topic.getIntValue("id");
		boolean locked = lock(topicId);
		if(locked) {
			LOG.info("锁定话题,开始计算话题");
			Elements scripts = Jsoup.parse(topic.getString("html")).select("pre[class=\"mathematica hljs\"]");
			JSONObject json = evaluate(topicId, scripts);
			setResult(topicId, json);
			unlock(topicId);
			LOG.info("设置话题结果完成,解除锁定");
		}else{
			LOG.info("锁定话题失败，可能被别的内核抢到了");
			return;
		}
	}
	
	public void create(int topicId) throws SessionException, SQLException, MathLinkException, IOException {
		JSONObject topic = manager.selectOne("topic.select", topicId);
		create(topic);
	}
	
	public void update(int topicId) throws IOException, SQLException, MathLinkException, IOException {
		JSONObject topic = manager.selectOne("topic.select", topicId);
		if(topic == null) {
			throw new SQLException("topic is null");
		}
		int status = topic.getIntValue("status");
		delete(topicId);
		if(status == Status.NO_CODE.value) {
			return;
		}
		create(topicId);
	}
	
	public void delete(int topicId) throws IOException {
		String replyPath = PATH + topicId + "/";
		File file = new File(replyPath);
		if (file.exists()) {
			Path path = Paths.get(replyPath);
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e)
						throws IOException {
					if (e == null) {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					} else {
						throw e;
					}
				}
			});
		}
	}
	
	@JedisCycle
	public boolean lock(int topicId) throws SessionException, SQLException {
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
	
	@JedisCycle
	public void unlock(int topicId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		jedis.del(LOCK + topicId);
	}
	
	
	public void reset(int topicId) throws SessionException, SQLException {
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", Status.WAITING.value);
		manager.update("topic.update", map);
		unlock(topicId);
	}
	
	@KernelCycle
	public JSONObject evaluate(int topicId, Elements scripts) throws SessionException, MathLinkException, IOException {
		String topicPath = PATH + topicId + "/";
		Path path = Paths.get(topicPath);
		Files.createDirectory(path);
		Status status = Status.SUCCESS;
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(topicPath, scripts.get(i).text());
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
		topicResult.put("status", status.value);
		topicResult.put("result", result);
		File file = new File(topicPath);
		if (file.list().length == 0) {
			Files.delete(path);
		}
		return topicResult;
	}
	
	public void setResult(int topicId, JSONObject topicResult) throws SessionException, SQLException {
		int status = topicResult.getIntValue("status");
		String result = topicResult.getJSONArray("result").toJSONString();
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", topicId);
		map.put("status", status);
		map.put("result", result);
		manager.update("topic.update", map);
	}

	public enum Status {

		ABORT(-2), SYNTAX_ERROR(-1), WAITING(0), EVALUATE(1), SUCCESS(2), NO_CODE(3);

		private int value;

		private Status(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}
}