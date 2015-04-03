package com.umbrella.service.beanstalkd;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.kit.TopicKit;
import com.umbrella.session.SessionException;

public class BeanstalkdTopicService extends BeanstalkdService{
	
	public BeanstalkdTopicService() {
		super("kernel-topic", LogManager.getLogger("beanstalkd-kernel-topic"));
	}
	
	@Inject private TopicKit kit;
	
	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		int topicId = Integer.parseInt(job.getData());
		if (kit.lockTopic(topicId)) {
			JSONObject topicResult = null;
			Elements scripts = kit.getScriptElements(topicId);
			LOG.info("开始计算话题");
			topicResult = kit.evaluate(topicId, scripts);
			kit.setResult(topicId, topicResult);
			LOG.info("设置话题结果完成");
		} else {
			LOG.info("锁定话题失败，可能被别的内核抢到了");
		}
	}

	@Override
	protected void exception(BeanstalkdJob job) throws SessionException, SQLException {
		int topicId = Integer.parseInt(job.getData());
		kit.resetTopicStatus(topicId);
	}
}
