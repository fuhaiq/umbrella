package com.umbrella.kit;

import static com.mongodb.client.model.Filters.eq;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.pegdown.PegDownProcessor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.mongo.MongoCycle;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

import redis.clients.jedis.Jedis;

public class TopicKit {
	
	@Inject private Session<Jedis> jedisSession;
	
	@Inject private Session<MongoClient> mongoSession;
	
	@Inject private Kernel kernel;
	
	private final String LOCK = "topic:lock:";
	
	private final static String PATH = "/home/wesker/git/umbrella-web/public/kernel/topic/";
	
	private final Logger LOG = LogManager.getLogger("topickit");
	
	@MongoCycle
	public void create(String tid, String mainpid) throws SessionException, MathLinkException, IOException {
		MongoClient mongo = mongoSession.get();
		MongoDatabase db = mongo.getDatabase("umbrella");
		Document topic = db.getCollection("objects").find(eq("_key", "topic:" + tid)).first();
		if(Objects.isNull(topic)) {
			throw new IllegalStateException("话题不存在,可能被用户删除.");
		}
		int status = topic.getInteger("status");
		if(status != Status.WAITING.value) {
			throw new IllegalStateException("话题状态错误["+ status +"], 期望值: " + Status.WAITING.getValue());
		}
		
		Document mainPost = db.getCollection("objects").find(eq("_key", "post:" + mainpid)).first();
		if(Objects.isNull(mainPost)) {
			throw new IllegalStateException("话题主回复为空.");
		}
		boolean locked = lock(tid);
		if(locked) {
			LOG.info("锁定话题,开始计算话题");
			PegDownProcessor processor = new PegDownProcessor();
			String content = mainPost.getString("content");
			String html = processor.markdownToHtml(content);
			Elements scripts = Jsoup.parse(html).select("code");
			JSONObject json = evaluate(tid, scripts);
			setResult(tid, json);
			unlock(tid);
			LOG.info("设置话题结果完成,解除锁定");
		}else{
			LOG.info("锁定话题失败，可能被别的内核抢到了");
			return;
		}
	}
	
	public void update(String tid, String mainpid) throws IOException, MathLinkException, IOException {
		Document topic = mongoSession.get().getDatabase("umbrella").getCollection("objects").find(eq("_key", "topic:" + tid)).first();
		if(Objects.isNull(topic)) {
			throw new IllegalStateException("话题不存在,可能被用户删除.");
		}
		int status = topic.getInteger("status");
		delete(tid);
		if(status == Status.NO_CODE.value) {
			return;
		}
		create(tid, mainpid);
	}
	
	public void delete(String tid) throws IOException {
		String replyPath = PATH + tid + "/";
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
	@MongoCycle
	public boolean lock(String tid) throws SessionException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(LOCK + tid, "lock");
		if(response == 0) {
			return false;
		}
		mongoSession.get().getDatabase("umbrella").getCollection("objects").updateOne(eq("_key", "topic:" + tid), new Document("$set", new Document("status", Status.EVALUATE.getValue())));
		return true;
	}
	
	@JedisCycle
	public void unlock(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		jedis.del(LOCK + topicId);
	}
	
	@MongoCycle
	public void reset(String tid) throws SessionException, SQLException {
		mongoSession.get().getDatabase("umbrella").getCollection("objects").updateOne(eq("_key", "topic:" + tid), new Document("$set", new Document("status", Status.WAITING.getValue())));
		unlock(tid);
	}
	
	@KernelCycle
	public JSONObject evaluate(String tid, Elements scripts) throws SessionException, MathLinkException, IOException {
		String topicPath = PATH + tid + "/";
		Path path = Paths.get(topicPath);
		Files.createDirectory(path);
		Status status = Status.SUCCESS;
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			String script = scripts.get(i).text();
			script = script.substring(3, script.length());
			JSONArray json = kernel.evaluate(topicPath, script);
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
	
	public void setResult(String tid, JSONObject topicResult) throws SessionException {
		int status = topicResult.getIntValue("status");
		JSONArray result = topicResult.getJSONArray("result");
		mongoSession.get().getDatabase("umbrella").getCollection("objects").updateOne(eq("_key", "topic:" + tid), new Document("$set", new Document("status", status).append("result", result)));
	}

	public enum Status {

		ABORT(-2), SYNTAX_ERROR(-1), NO_CODE(0), WAITING(1), EVALUATE(2), SUCCESS(3);

		private int value;

		private Status(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}
}