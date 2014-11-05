package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
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
		LOG.info("beanstalkd service starts");
	}

	@Override
	protected void shutDown() throws Exception {
		pool.invalidateObject(bean);
		LOG.info("beanstalkd service stops");
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
				execute(kernelJob);
				bean.delete(kernelJob.getId());
			}
		}
	}
	
	public void execute(BeanstalkdKernelJob kernelJob) throws MathLinkException {
		Document html = getTopicHtml(kernelJob.getTopicId());
		List<String> scripts = analysis(html);
		JSON json = null;
		try {
			json = process(scripts);
		} catch (SessionException e) {
			//TODO check NoSuchElementException exception, no kernel currently. then release job with higher priority.
		} catch (MathLinkException e) {
			throw e;
		}
		setTopicResult(kernelJob.getTopicId(), json);
	}
	
	@JedisTransaction
	public Document getTopicHtml(String topicId) throws SessionException {
		Jedis jedis = jedisSession.get();
		String html = checkNotNull(jedis.hget("topic:" + topicId, "content"), "topic content is empty");
		return Jsoup.parse(html);
	}
	
	private List<String> analysis(Document html) {
		//TODO analysis all mathematica script
		return null;
	}
	
	@KernelTransaction
	public JSON process(List<String> scripts) throws SessionException, MathLinkException {
		JSONArray json = new JSONArray();
		process:for(int position = 0; position < scripts.size(); position++) {
			JSONObject row = new JSONObject();
			String script = scripts.get(position);
			row.put("position", position);
			JSONArray results = kernel.evaluate(script);
			row.put("data", results);
			json.add(row);
			for(int i = 0; i < results.size(); i ++) {
				JSONObject result = results.getJSONObject(i);
				String type = result.getString("type");
				if("error".equals(type) || "abort".equals(type)) {
					break process;
				}
			}
		} 
		return json;
	}
	
	@JedisTransaction
	public long setTopicResult(String topicId, JSON result) throws SessionException {
		checkNotNull(result, "topic result is empty");
		Jedis jedis = jedisSession.get();
		return jedis.hset("topic:" + topicId, "result", result.toJSONString());
	}
}
