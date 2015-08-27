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
	
	private static final int RELEASE_SEC = 5;
	
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
					if(execute(job)) {
						bean.release(job.getId(), (long)(Math.pow(2, 32) -1), RELEASE_SEC);
						LOG.info(RELEASE_SEC + "秒钟后重新调度任务[id:"+job.getId()+"]");
					} else {
						bean.delete(job.getId());
					}
				} catch(Exception e) {
					LOG.error("执行任务发生错误,隐藏任务[id:"+job.getId()+"]: ", e);
					bean.bury(job.getId(), (long) Math.pow(2, 31));
				}
			}
		}
	}
	
	protected abstract boolean execute(BeanstalkdJob job) throws Exception;
	
}
