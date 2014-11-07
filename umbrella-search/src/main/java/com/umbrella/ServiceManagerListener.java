package com.umbrella;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.beanstalkd.BeanstalkdConfig;

public class ServiceManagerListener extends ServiceManager.Listener{

	private final Logger LOG = LogManager.getLogger(ServiceManagerListener.class);
	
	@Inject
	private ObjectPool<Beanstalkd> beans;
	
	@Inject
	private BeanstalkdConfig beansConfig;
	
	@Override
	public void healthy() {
		LOG.info("service manager starts successfully.");
	}
	
	@Override
	public void stopped() {
		try {
			beans.clear();
			beans.close();
			beansConfig.getGroup().shutdownGracefully();
		} catch (Exception dontCare) {}
		LOG.info("service manager stops.");
	}

}
