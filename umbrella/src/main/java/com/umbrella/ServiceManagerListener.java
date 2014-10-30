package com.umbrella;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.beanstalkd.BeanstalkdConfig;
import com.wolfram.jlink.KernelLink;

public class ServiceManagerListener extends ServiceManager.Listener{

	private final Logger LOG = LogManager.getLogger(ServiceManagerListener.class);
	
	@Inject
	private ObjectPool<KernelLink> kernel;
	
	@Inject
	private ObjectPool<Jedis> jedis;
	
	@Inject
	private ObjectPool<Beanstalkd> beans;
	
	@Inject
	private BeanstalkdConfig beansConfig;
	
	@Inject
	private Provider<ServiceManager> manager;
	
	@Override
	public void healthy() {
		LOG.info("service manager starts successfully.");
	}
	
	@Override
	public void failure(Service service) {
		LOG.error("service failure >> " + service.toString());
		manager.get().stopAsync();
	}
	
	@Override
	public void stopped() {
		try {
			kernel.clear();
			kernel.close();
			jedis.clear();
			jedis.close();
			beans.clear();
			beans.close();
			beansConfig.getGroup().shutdownGracefully();
		} catch (Exception dontCare) {}
		LOG.info("service manager stops.");
	}

}
