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
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.mongo.MongoCycle;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

import redis.clients.jedis.Jedis;

public class ReplyKit {
	
	@Inject private Session<Jedis> jedisSession;
	
	@Inject private Session<MongoClient> mongoSession;
	
	@Inject private Kernel kernel;
	
	private final String LOCK = "reply:lock:";
	
	private final static String PATH = "/home/wesker/umbrella/assets/reply/";
	
	private final Logger LOG = LogManager.getLogger("replykit");
	
	@MongoCycle
	public void create(String replyid) throws SessionException, MathLinkException, IOException {
		Document reply = mongoSession.get().getDatabase("umbrella").getCollection("reply").find(eq("_id", new ObjectId(replyid))).first();
		if(Objects.isNull(reply)) {
			throw new IllegalStateException("reply is null");
		}
		int status = reply.getInteger("status");
		if(status != Status.WAITING.value) {
			throw new IllegalStateException("bad reply status["+ status +"], expect " + Status.WAITING.getValue());
		}
		boolean locked = lock(replyid);
		if(locked) {
			LOG.info("锁定回复,开始计算回复");
			Elements scripts = Jsoup.parse(reply.getString("html")).select("pre[class=\"mathematica hljs\"]");
			JSONObject json = evaluate(replyid, scripts);
			setResult(replyid, json);
			unlock(replyid);
			LOG.info("设置回复结果完成,解除锁定");
		}else{
			LOG.info("锁定回复失败，可能被别的内核抢到了");
			return;
		}
	}
	
	public void update(String replyid) throws IOException, MathLinkException, IOException {
		Document reply = mongoSession.get().getDatabase("umbrella").getCollection("reply").find(eq("_id", new ObjectId(replyid))).first();
		if(Objects.isNull(reply)) {
			throw new IllegalStateException("reply is null");
		}
		int status = reply.getInteger("status");
		delete(replyid);
		if(status == Status.NO_CODE.value) {
			return;
		}
		create(replyid);
	}
	
	public void delete(String replyid) throws IOException {
		String replyPath = PATH + replyid + "/";
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
	public boolean lock(String replyid) throws SessionException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(LOCK + replyid, "lock");
		if(response == 0) {
			return false;
		}
		mongoSession.get().getDatabase("umbrella").getCollection("reply").updateOne(eq("_id", new ObjectId(replyid)), new Document("$set", new Document("status", Status.EVALUATE.getValue())));
		return true;
	}
	
	@JedisCycle
	public void unlock(String replyid) throws SessionException {
		Jedis jedis = jedisSession.get();
		jedis.del(LOCK + replyid);
	}
	
	
	public void reset(String replyid) throws SessionException, SQLException {
		mongoSession.get().getDatabase("umbrella").getCollection("reply").updateOne(eq("_id", new ObjectId(replyid)), new Document("$set", new Document("status", Status.WAITING.getValue())));
		unlock(replyid);
	}
	
	@KernelCycle
	public JSONObject evaluate(String replyid, Elements scripts) throws SessionException, MathLinkException, IOException {
		String replyPath = PATH + replyid + "/";
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
	
	public void setResult(String replyid, JSONObject replyResult) throws SessionException {
		int status = replyResult.getIntValue("status");
		JSONArray result = replyResult.getJSONArray("result");
		mongoSession.get().getDatabase("umbrella").getCollection("reply").updateOne(eq("_id", new ObjectId(replyid)), new Document("$set", new Document("status", status).append("result", result)));
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