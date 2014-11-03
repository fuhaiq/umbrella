package com.umbrella.service.beanstalkd;

import org.apache.commons.pool2.ObjectPool;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.fastjson.JSONArray;
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
				BeanstalkdDBJob dbJob = new BeanstalkdDBJob(job);
				execute(dbJob);
				bean.delete(dbJob.getId());
			}
		}
	}
	
	private void execute(BeanstalkdDBJob job) {
		JSONArray sqls = job.getSqls();
		if(sqls.size() > 1) {
			executeWithTransaction(job.getId(), sqls);
		} else if(sqls.size() == 1) {
			executeWithoutTransaction(job.getId(), sqls.getJSONObject(0));
		} else {
			throw new IllegalStateException("unknown job size in DB job " + job.getId());
		}
	}
	
	private void executeWithoutTransaction(long id, JSONObject sql) {
		String key = sql.getString("key");
		if (Strings.isNullOrEmpty(key)) throw new IllegalStateException("no key in DB job " + id);
		JSONObject data = sql.getJSONObject("data");
		if (key.contains("insert")) {
			manager.insert(key, data);
		} else if (key.contains("update")) {
			manager.update(key, data);
		} else if (key.contains("delete")) {
			manager.delete(key, data);
		} else {
			throw new IllegalStateException("undefined type of key[" + key + "] in DB job " + id);
		}
	}
	
	private void executeWithTransaction(long id, JSONArray sqls) {
		manager.startManagedSession(TransactionIsolationLevel.READ_UNCOMMITTED);
		try {
			for (int i = 0; i < sqls.size(); i++) {
				executeWithoutTransaction(id, sqls.getJSONObject(i));
			}
			manager.commit();
		} catch (Throwable e) {
			manager.rollback(true);
			throw e;
		} finally {
			manager.close();
		}

	}
}
