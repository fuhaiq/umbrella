package com.umbrella.service.beanstalkd;

import java.sql.SQLException;

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
import com.umbrella.service.beanstalkd.BeanstalkdKernelManager.Status;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.MathLinkException;

public class BeanstalkdKernelService extends AbstractExecutionThreadService{
	
	private final Logger LOG = LogManager.getLogger("beanstalkd-kernel-service");
	
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
	
	
	public boolean execute(BeanstalkdKernelJob kernelJob) throws MathLinkException, SessionException, SQLException {
		if (manager.lockTopic(kernelJob.getTopicId())) {
			JSONObject topicResult = null;
			Elements scripts = manager.getScriptElements(kernelJob.getTopicId());
			if (scripts == null || scripts.size() == 0) {
				LOG.info("没有可执行的代码，直接返回");
				topicResult = new JSONObject();
				topicResult.put("status", Status.SUCCESS.getValue());
				topicResult.put("result", new JSONArray());
			} else {
				LOG.info("开始计算话题");
				try {
					topicResult = manager.evaluate(scripts);
				} catch (SessionException | MathLinkException e) {
					manager.resetTopicStatus(kernelJob.getTopicId());
					if ("Timeout waiting for idle object".equals(e.getMessage())) {
						LOG.info("目前没有可用的kernel,重置job");
						return false;
					} else if("MathLink connection was lost.".equals(e.getMessage())) {
						LOG.info("kernel计算超时,销毁kernel,重置job");
						return false;
					} else {
						throw e;
					}
				}
			}
			manager.setResult(kernelJob.getTopicId(), topicResult);
			LOG.info("设置话题结果完成");
			return true;
		} else {
			LOG.info("锁定话题失败，可能被别的内核抢到了");
			return true;
		}
	}
}
