package com.umbrella.service.beanstalkd;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.service.beanstalkd.BeanstalkdKernelManager.Status;
import com.umbrella.session.SessionException;

public class BeanstalkdKernelService extends BeanstalkdService{
	
	public BeanstalkdKernelService() {
		super("kernel", LogManager.getLogger("beanstalkd-kernel-service"));
	}
	
	@Inject private BeanstalkdKernelManager manager;
	
	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		int topicId = Integer.parseInt(job.getData());
		if (manager.lockTopic(topicId)) {
			JSONObject topicResult = null;
			Elements scripts = manager.getScriptElements(topicId);
			if (scripts == null || scripts.size() == 0) {
				LOG.info("没有可执行的代码，直接返回");
				topicResult = new JSONObject();
				topicResult.put("status", Status.SUCCESS.getValue());
				topicResult.put("result", new JSONArray());
			} else {
				LOG.info("开始计算话题");
				topicResult = manager.evaluate(scripts);
			}
			manager.setResult(topicId, topicResult);
			LOG.info("设置话题结果完成");
		} else {
			LOG.info("锁定话题失败，可能被别的内核抢到了");
		}
	}

	@Override
	protected void exception(BeanstalkdJob job) throws SessionException, SQLException {
		int topicId = Integer.parseInt(job.getData());
		manager.resetTopicStatus(topicId);
	}
}
