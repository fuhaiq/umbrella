package com.umbrella.service.beanstalkd;

import java.sql.SQLException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;

public class BeanstalkdDBService extends AbstractExecutionThreadService{
	
	private final Logger LOG = LogManager.getLogger(BeanstalkdDBService.class);
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	@Inject private SqlSessionManager manager;
	
	private Beanstalkd bean;
	
	
	@Override
	protected void startUp() throws Exception {
		bean = pool.borrowObject();
		bean.watch("db");
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
				insert();
			}
		}
	}
	
	public void insert() throws SQLException {
		JSONObject param = new JSONObject();
		param.put("name", "michael");
		param.put("age", 33);
		manager.insert("user.insert", param);
	}

}
