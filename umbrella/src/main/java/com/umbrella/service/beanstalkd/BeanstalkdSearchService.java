package com.umbrella.service.beanstalkd;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;

public class BeanstalkdSearchService extends AbstractExecutionThreadService {

private final Logger LOG = LogManager.getLogger(BeanstalkdSearchService.class);
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	private Beanstalkd bean;
	
	@Override
	protected void startUp() throws Exception {
		bean = pool.borrowObject();
		bean.watch("search");
		LOG.info("beanstalkd search service starts");
	}

	@Override
	protected void shutDown() throws Exception {
		pool.invalidateObject(bean);
		LOG.info("beanstalkd search service stops");
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
				BeanstalkdSearchJob searchJob = new BeanstalkdSearchJob(job);
				bean.delete(searchJob.getId());
			}
		}
	}

}
