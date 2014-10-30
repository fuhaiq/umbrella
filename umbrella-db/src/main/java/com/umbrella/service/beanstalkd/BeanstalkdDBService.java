package com.umbrella.service.beanstalkd;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.db.mapper.UserMapper;
import com.umbrella.db.model.User;

public class BeanstalkdDBService extends AbstractExecutionThreadService{
	
	private final Logger LOG = LogManager.getLogger(BeanstalkdDBService.class);
	
	@Override
	public String toString() {
		return super.toString();
	}

	@Inject
	private ObjectPool<Beanstalkd> pool;
	
	private Beanstalkd bean;
	
	@Inject private UserMapper mapper;
	
	@Override
	protected void startUp() throws Exception {
		bean = pool.borrowObject();
		bean.watch("kernel");
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
				System.out.println("->"+job+"<-");
				User user = new User();
				user.setAge(30);
				user.setName("mike");
				mapper.insert(user);
			}
		}
	}

}
