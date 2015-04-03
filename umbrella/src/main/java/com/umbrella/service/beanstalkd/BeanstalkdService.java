package com.umbrella.service.beanstalkd;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.beanstalkd.BeanstalkdJob;

public abstract class BeanstalkdService extends AbstractExecutionThreadService {

	protected final Logger LOG;
	
	@Inject private ObjectPool<Beanstalkd> pool;
	
	protected Beanstalkd bean;
	
	private final String tube;
	
	protected BeanstalkdService(String tube, Logger LOG) {
		this.tube = tube;
		this.LOG = LOG;
	}
	
	@Override
	protected final void startUp() throws Exception {
		bean = pool.borrowObject();
		bean.watch(tube);
		LOG.info("beanstalkd service starts");
	}

	@Override
	protected final void shutDown() throws Exception {
		pool.invalidateObject(bean);
		LOG.info("beanstalkd service stops");
	}
	
	@Override
	protected final void triggerShutdown() {
		bean.close();
	}
	
	@Override
	protected final void run() throws InterruptedException, ExecutionException, TimeoutException {
		while (isRunning()) {
			BeanstalkdJob job = bean.reserve();
			if(!Objects.isNull(job)) {
				try {
					execute(job);
					bean.delete(job.getId());
				} catch(Exception e) {
					LOG.error("exception when exec bean job, bury the job[id:"+job.getId()+"]: ", e);
					try {
						exception(job);
					} catch (Exception catchEx) {
						LOG.error("exception when exec exception method: ", catchEx);
					}
					bean.bury(job.getId(), (long) Math.pow(2, 31));
				}
			}
		}
	}
	
	protected abstract void execute(BeanstalkdJob job) throws Exception;
	
	protected void exception(BeanstalkdJob job) throws Exception {}
}
