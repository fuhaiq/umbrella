package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.kernel.Kernel;
import com.umbrella.kernel.KernelTransaction;
import com.umbrella.redis.JedisTransaction;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class BeanstalkdKernelService extends AbstractExecutionThreadService{
	
	private final Logger LOG = LogManager.getLogger(BeanstalkdKernelService.class);
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	@Inject private Session<Jedis> jedisSession;
	
	@Inject private Kernel kernel;
	
	private Beanstalkd bean;
	
	@Override
	protected void startUp() throws Exception {
		bean = pool.borrowObject();
		bean.watch("kernel");
		LOG.info("beanstalkd kernel service starts");
	}

	@Override
	protected void shutDown() throws Exception {
		pool.invalidateObject(bean);
		LOG.info("beanstalkd kernel service stops");
	}

	@Override
	protected void triggerShutdown() {
		bean.close();
	}

	@Override
	protected void run() throws Exception {
		while (isRunning()) {
			String job = bean.reserve();
			if(!Strings.isNullOrEmpty(job)) {
				BeanstalkdKernelJob kernelJob = new BeanstalkdKernelJob(job);
				if(shouldEvaluate(kernelJob.getTopicId())) {
					LOG.info("TOPIC没有结果数据,开始计算....");
					execute(kernelJob);
				} else {
					LOG.info("TOPIC已经有数据,跳过计算");
				}
				bean.delete(kernelJob.getId());
			}
		}
	}
	
	public void execute(BeanstalkdKernelJob kernelJob) throws MathLinkException, InterruptedException, ExecutionException, TimeoutException {
		Elements scripts = getScriptElements(kernelJob.getTopicId());
		JSONObject json = null;
		if(scripts.size() == 0) {
			json = new JSONObject();
			json.put("status", Status.SUCCESS.getValue());
			json.put("result", new JSONArray());
		} else {
			try {
				json = process(scripts);
			} catch (SessionException e) {
				if("Timeout waiting for idle object".equals(e.getMessage())) {
					bean.release(kernelJob.getId(), (int) Math.pow(2, 31), 0);
					return;
				} else {
					throw e;
				}
			}
		}
		setTopicResult(kernelJob.getTopicId(), json);
	}
	
	@JedisTransaction
	public boolean shouldEvaluate(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		String status = jedis.hget("topic:" + topicId, "status");
		return status.equals(Status.WAITING.getValue());
	}
	
	@JedisTransaction
	public Elements getScriptElements(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		String html = checkNotNull(jedis.hget("topic:" + topicId, "html"), "topic html is empty");
		return Jsoup.parse(html).select("pre[class=brush:mathematica;toolbar:false]");
	}
	
	@KernelTransaction
	public JSONObject process(Elements scripts) throws SessionException, MathLinkException {
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
	
	@JedisTransaction
	public String setTopicResult(String topicId, JSONObject topicResult) throws SessionException {
		checkNotNull(topicResult, "topic result is empty");
		Jedis jedis = jedisSession.get();
		String status = checkNotNull(topicResult.getString("status"), "topic status is null");
		String result = checkNotNull(topicResult.getJSONArray("result").toJSONString(), "topic result is null");
		Map<String, String> data = Maps.newHashMap();
		data.put("status", status);
		data.put("result", result);
		return jedis.hmset("topic:" + topicId, data);
	}
}
