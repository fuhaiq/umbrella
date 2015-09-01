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
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.umbrella.UmbrellaConfig;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelConfig;
import com.umbrella.kernel.KernelCycle;
import com.umbrella.mongo.MongoCycle;
import com.umbrella.redis.JedisCycle;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

import redis.clients.jedis.Jedis;

public class PostKit {
	
	@Inject private Session<Jedis> jedisSession;
	
	@Inject private Session<MongoClient> mongoSession;
	
	@Inject private Kernel kernel;
	
	@Inject private UmbrellaConfig umbrella;
	
	public static final String LOCK = "post:lock:";
	
	private final Logger LOG = LogManager.getLogger("postkit");
	
	@MongoCycle
	public boolean create(String pid) throws SessionException, MathLinkException, IOException {
		Document post = mongoSession.get().getDatabase("umbrella").getCollection("objects").find(eq("_key", "post:" + pid)).first();
		if(Objects.isNull(post)) {
			throw new IllegalStateException("回复不存在.");
		}
		int status = post.getInteger("status");
		if(status != Status.WAITING.value) {
			throw new IllegalStateException("回复状态错误["+ status +"], 期望值: " + Status.WAITING.getValue());
		}
		boolean locked = lock(pid);
		if(locked) {
			try{
				LOG.info("[计算]锁定回复.");
				mongoSession.get().getDatabase("umbrella").getCollection("objects").updateOne(eq("_key", "post:" + pid), new Document("$set", new Document("status", Status.EVALUATE.getValue())));
				@SuppressWarnings("unchecked")
				List<String> scripts = post.get("code", List.class);
				JSONObject json = evaluate(pid, scripts);
				setResult(pid, json);
			} catch(Exception e) {
				mongoSession.get().getDatabase("umbrella").getCollection("objects").updateOne(eq("_key", "post:" + pid), new Document("$set", new Document("status", Status.WAITING.getValue())));
				throw e;
			} finally {
				unlock(pid);
				LOG.info("[计算]解除锁定.");
			}
		}else{
			LOG.info("[计算]锁定回复失败，被别的计算内核抢到|此回复正在被删除(不需要计算)");
		}
		return false;
	}
	
	@MongoCycle
	public boolean update(String pid) throws IOException, MathLinkException, IOException {
		Document post = mongoSession.get().getDatabase("umbrella").getCollection("objects").find(eq("_key", "post:" + pid)).first();
		if(Objects.isNull(post)) {
			throw new IllegalStateException("回复不存在.");
		}
		int status = post.getInteger("status");
		boolean needRelease = delete(pid);
		if(needRelease) {
			return true;
		}
		if(status == Status.NO_CODE.value) {
			return false;
		}
		return create(pid);
	}
	
	public boolean delete(String pid) throws IOException {
		KernelConfig config = umbrella.getKernel();
		String postPath = config.getImgDir() + "/" + pid + "/";
		boolean locked = lock(pid);
		if(locked) {
			try{
				LOG.info("[删除]锁定回复.");
				File file = new File(postPath);
				if (file.exists()) {
					Path path = Paths.get(postPath);
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
			} finally {
				unlock(pid);
				LOG.info("[删除]解除锁定.");
			}
			return false;
		} else {
			LOG.info("[删除]锁定回复失败，此回复正在计算.");
			return true;
		}
		
	}
	
	@JedisCycle
	public boolean lock(String pid) throws SessionException {
		Jedis jedis = jedisSession.get();
		long response = jedis.setnx(LOCK + pid, "lock");
		if(response == 0) {
			return false;
		}
		return true;
	}
	
	@JedisCycle
	public void unlock(String pid) throws SessionException {
		Jedis jedis = jedisSession.get();
		jedis.del(LOCK + pid);
	}
	
	@KernelCycle
	public JSONObject evaluate(String pid, List<String> scripts) throws SessionException, MathLinkException, IOException {
		KernelConfig config = umbrella.getKernel();
		String postPath = config.getImgDir() + "/" + pid + "/";
		Path path = Paths.get(postPath);
		Files.createDirectories(path);
		Status status = Status.SUCCESS;
		JSONArray result = new JSONArray();
		outer:for(int i = 0; i < scripts.size(); i++) {
			JSONArray json = kernel.evaluate(scripts.get(i));
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
		JSONObject postResult = new JSONObject();
		postResult.put("status", status.value);
		postResult.put("result", result);
		File file = new File(postPath);
		if (file.list().length == 0) {
			Files.delete(path);
		}
		return postResult;
	}
	
	public void setResult(String pid, JSONObject postResult) throws SessionException {
		int status = postResult.getIntValue("status");
		JSONArray result = postResult.getJSONArray("result");
		mongoSession.get().getDatabase("umbrella").getCollection("objects").updateOne(eq("_key", "post:" + pid), new Document("$set", new Document("status", status).append("result", result)));
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