package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;

import com.alibaba.fastjson.JSON;
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
		JSONObject topic = JSON.parseObject(job.getData());
		LOG.info(topic.toJSONString());
		String tid = checkNotNull(topic.getString("tid"), "no topic's id with job:" + job.getId());
		String mainpid = checkNotNull(topic.getString("mainpid"), "no topic's mainpid with job:" + job.getId());
		String action = checkNotNull(topic.getString("action"), "no action with job:" + job.getId());
		if (action.equals("create")) {
			kit.create(tid, mainpid);
			LOG.info("创建话题完成");
		} else if (action.equals("delete")) {
			kit.delete(tid);
			LOG.info("删除话题完成");
		} else if (action.equals("update")) {
			kit.update(tid, mainpid);
			LOG.info("更新话题完成");
		} else {
			throw new IllegalStateException("BAD topic action:" + action);
		}
	}

	@Override
	protected void exception(BeanstalkdJob job) throws SessionException, SQLException {
		JSONObject topic = JSON.parseObject(job.getData());
		String topicId = checkNotNull(topic.getString("tid"), "no topic's id with job:" + job.getId());
		String action = checkNotNull(topic.getString("action"), "no action with job:" + job.getId());
		if (action.equals("create") || action.equals("update")) {
			kit.reset(topicId);
		}
	}
}
