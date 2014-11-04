package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import redis.clients.jedis.Jedis;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.kernel.Kernel;
import com.umbrella.redis.JedisTransaction;
import com.umbrella.session.Session;

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
			}
		}
	}
	
	public void execute(BeanstalkdKernelJob kernelJob) {
		
	}
	
	@JedisTransaction
	public Document getTopicHtml(String topicId) {
		Jedis jedis = jedisSession.get();
		String html = checkNotNull(jedis.hget("topic:" + topicId, "content"), "topic content is empty");
		return Jsoup.parse(html);
	}
	
	private List<String> analysis(Document html) {
		//TODO analysis all mathematica script
		return null;
	}
}
