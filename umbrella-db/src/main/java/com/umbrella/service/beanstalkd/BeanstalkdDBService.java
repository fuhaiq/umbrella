package com.umbrella.service.beanstalkd;

import static com.google.common.base.Preconditions.checkNotNull;

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
		LOG.info("beanstalkd db service starts");
	}

	@Override
	protected void shutDown() throws Exception {
		pool.invalidateObject(bean);
		LOG.info("beanstalkd db service stops");
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
		switch (sqls.size()) {
		case 1:
			executeWithoutTransaction(job.getId(), sqls.getJSONObject(0));
			break;
		case 0:
			throw new IllegalStateException("no sql in DB job " + job.getId());
		default:
			executeWithTransaction(job.getId(), sqls);
			break;
		}
	}
	
	private void executeWithoutTransaction(long id, JSONObject sql) {
		String key = checkNotNull(sql.getString("key"), "no key in DB job " + id);
		String type = checkNotNull(sql.getString("type"), "no type in DB job " + id);
		JSONObject data = sql.getJSONObject("data");
		switch (type) {
		case "i":
			manager.insert(key, data);
			break;
		case "u":
			manager.update(key, data);
			break;
		case "d":
			manager.delete(key, data);
			break;
		default:
			throw new IllegalStateException("undefined type [" + type + "] in DB job " + id);
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
