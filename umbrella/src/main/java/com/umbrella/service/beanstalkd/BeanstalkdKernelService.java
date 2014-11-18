package com.umbrella.service.beanstalkd;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;

import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

import com.umbrella.service.beanstalkd.BeanstalkdKernelManager.Status;

public class BeanstalkdKernelService extends AbstractExecutionThreadService{
	
	private final Logger LOG = LogManager.getLogger(BeanstalkdKernelService.class);
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	@Inject private BeanstalkdKernelManager manager;
	
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
				if(execute(kernelJob)) {
					bean.delete(kernelJob.getId());
				} else {
					bean.release(kernelJob.getId(), (long) Math.pow(2, 31), 0);
				}
			}
		}
	}
	
	
	public boolean execute(BeanstalkdKernelJob kernelJob) throws MathLinkException, InterruptedException, ExecutionException, TimeoutException {
		String updated = manager.setToEvaluateThenGetUpdated(kernelJob.getTopicId());
		if (updated != null) {
			JSONObject topicResult = null;
			Elements scripts = manager.getScriptElements(kernelJob.getTopicId());
			if (scripts == null) {
				return true;
			} else if (scripts.size() == 0) {
				topicResult = new JSONObject();
				topicResult.put("status", Status.SUCCESS.getValue());
				topicResult.put("result", new JSONArray());
			} else {
				try {
					topicResult = manager.evaluate(scripts);
				} catch (SessionException e) {
					if ("Timeout waiting for idle object".equals(e.getMessage())) {
						LOG.info("目前没有可用的kernel,重置job");
						manager.resetTopicStatus(kernelJob.getTopicId());
						return false;
					} else {
						throw e;
					}
				}
			}
			LOG.info("开始计算话题");
			manager.setResult(kernelJob.getTopicId(), topicResult, updated);
			return true;
		} else {
			return true;
		}
	}
}
