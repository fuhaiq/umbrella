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

public class ReplyKit {

	@Inject private Session<Jedis> jedisSession;

	@Inject private SqlSessionManager manager;

	@Inject private Kernel kernel;

	private final String LOCK = "reply:lock:";

	private final static String PATH = "/home/wesker/umbrella-openresty/www/static/reply/";
	
	private final Logger LOG = LogManager.getLogger("replykit");
	
	public void create(JSONObject reply) throws SessionException, SQLException, MathLinkException, IOException {
		if(Objects.isNull(reply)) {
			throw new SQLException("reply is null");
		}
		int status = reply.getIntValue("status");
		if(status != Status.WAITING.value) {
			throw new SQLException("bad reply status["+ status +"], expect " + Status.WAITING.getValue());
		}
		int replyId = reply.getIntValue("id");
		boolean locked = lock(replyId);
		if(locked) {
			LOG.info("锁定回复,开始计算回复");
			Elements scripts = Jsoup.parse(reply.getString("html")).select("pre[class=\"mathematica hljs\"]");
			JSONObject json = evaluate(replyId, scripts);
			setResult(replyId, json);
			unlock(replyId);
			LOG.info("设置回复结果完成,解除锁定");
		}else{
			LOG.info("锁定回复失败，可能被别的内核抢到了");
			return;
		}
	}
	
	public void create(int replyId) throws SessionException, SQLException, MathLinkException, IOException {
		JSONObject reply = manager.selectOne("reply.select", replyId);
		create(reply);
	}
	
	public void update(int replyId) throws IOException, SQLException, MathLinkException, IOException {
		JSONObject reply = manager.selectOne("reply.select", replyId);
		if(reply == null) {
			throw new SQLException("reply is null");
		}
		int status = reply.getIntValue("status");
		delete(replyId);
		if(status == Status.NO_CODE.value) {
			return;
		}
		create(reply);
	}
	
	public void delete(int replyId) throws IOException {
		String replyPath = PATH + replyId + "/";
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
	public boolean lock(int replyId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(LOCK + replyId, "lock");
		if(response == 0) {
			return false;
		}
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", replyId);
		map.put("status", Status.EVALUATE.value);
		manager.update("reply.update", map);
		return true;
	}
	
	@JedisCycle
	public void unlock(int replyId) throws SessionException, SQLException {
		Jedis jedis = jedisSession.get();
		jedis.del(LOCK + replyId);
	}
	
	public void reset(int replyId) throws SessionException, SQLException {
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", replyId);
		map.put("status", Status.WAITING.value);
		manager.update("reply.update", map);
		unlock(replyId);
	}
	
	@KernelCycle
	public JSONObject evaluate(int replyId, Elements scripts) throws SessionException, MathLinkException, IOException {
		String replyPath = PATH + replyId + "/";
		Path path = Paths.get(replyPath);
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
		replyResult.put("status", status.value);
		replyResult.put("result", result);
		File file = new File(replyPath);
		if (file.list().length == 0) {
			Files.delete(path);
		}
		return replyResult;
	}
	
	public void setResult(int replyId, JSONObject replyResult) throws SessionException, SQLException {
		int status = replyResult.getIntValue("status");
		String result = replyResult.getJSONArray("result").toJSONString();
		Map<String, Object> map = Maps.newHashMap();
		map.put("id", replyId);
		map.put("status", status);
		map.put("result", result);
		manager.update("reply.update", map);
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
