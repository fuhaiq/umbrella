package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.BeanstalkdJob;
import com.umbrella.kit.ElaSearchKit;

public class BeanstalkdSearchService extends BeanstalkdService {
	
	@Inject private ElaSearchKit kit;

	protected BeanstalkdSearchService() {
		super("search", LogManager.getLogger("beanstalkd-search"));
	}

	@Override
	protected void execute(BeanstalkdJob job) throws Exception {
		JSONObject json = JSON.parseObject(job.getData());
		int id = checkNotNull(json.getInteger("id"), "no id with search job:" + job.getId());
		String type = checkNotNull(json.getString("type"), "no type with search job:" + job.getId());
		String action = checkNotNull(json.getString("action"), "no action with search job:" + job.getId());
		switch(type){
			case "topic":
				indexTopic(id, action, job.getId());
				break;
			case "reply":
				indexReply(id, action, job.getId());
				break;
			default: 
				throw new IllegalStateException("no this type defined with search job:" + job.getId());
		}
	}
	
	private void indexTopic(int topicId, String action, long jobId) throws ClientProtocolException, SQLException, IOException {
		switch(action) {
		case "create":
			LOG.info("开始创建话题索引");
			kit.createTopic(topicId);
			LOG.info("创建话题索引完成");
			break;
		default:
			throw new IllegalStateException("no this action defined with search job:" + jobId);
		}
	}
	
	private void indexReply(int replyId, String action, long jobId) throws ClientProtocolException, SQLException, IOException {
		switch(action) {
		case "create":
			LOG.info("开始创建回复索引");
			kit.createReply(replyId);
			LOG.info("创建回复索引完成");
			break;
		default:
			throw new IllegalStateException("no this action defined with search job:" + jobId);
		}
	}
}
