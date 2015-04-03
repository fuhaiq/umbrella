package com.umbrella.service.beanstalkd;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.jsoup.select.Elements;

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
		int replyId = Integer.parseInt(job.getData());
		if (kit.lockReply(replyId)) {
			JSONObject topicResult = null;
			Elements scripts = kit.getScriptElements(replyId);
			LOG.info("开始计算回复");
			topicResult = kit.evaluate(replyId, scripts);
			kit.setResult(replyId, topicResult);
			LOG.info("设置回复结果完成");
		} else {
			LOG.info("锁定回复失败，可能被别的内核抢到了");
		}
	}
	
	
	@Override
	protected void exception(BeanstalkdJob job) throws SessionException, SQLException {
		int topicId = Integer.parseInt(job.getData());
		kit.resetReplyStatus(topicId);
	}

}
