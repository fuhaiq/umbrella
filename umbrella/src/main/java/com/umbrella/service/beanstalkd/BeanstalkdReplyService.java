package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.kit.ReplyKit;
import com.umbrella.session.SessionException;

public class BeanstalkdReplyService extends BeanstalkdService {

	protected BeanstalkdReplyService() {
		super("kernel-reply", LogManager.getLogger("beanstalkd-kernel-reply"));
	}

	@Inject private ReplyKit kit;
	
	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		JSONObject reply = JSON.parseObject(job.getData());
		String replyId = checkNotNull(reply.getString("id"), "no replyId with job:" + job.getId());
		String action = checkNotNull(reply.getString("action"), "no action with job:" + job.getId());
		if (action.equals("create")) {
			kit.create(replyId);
			LOG.info("创建回复完成");
		} else if (action.equals("delete")) {
			kit.delete(replyId);
			LOG.info("删除回复完成");
		} else if (action.equals("update")) {
			kit.update(replyId);
			LOG.info("更新回复完成");
		} else {
			throw new IllegalStateException("BAD reply action:" + action);
		}
	}
	
	
	@Override
	protected void exception(BeanstalkdJob job) throws SessionException, SQLException {
		JSONObject reply = JSON.parseObject(job.getData());
		String replyId = checkNotNull(reply.getString("id"), "no replyId with job:" + job.getId());
		String action = checkNotNull(reply.getString("action"), "no action with job:" + job.getId());
		if (action.equals("create") || action.equals("update")) {
			kit.reset(replyId);
		}
	}

}
