package com.umbrella;

import java.util.concurrent.ExecutorService;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.umbrella.beanstalkd.Beanstalkd;
import com.wolfram.jlink.KernelLink;

public class ServiceManagerListener extends ServiceManager.Listener{

	private final Logger LOG = LogManager.getLogger("service-manager");
	
	@Inject private ObjectPool<KernelLink> kernel;
	
	@Inject private ObjectPool<Jedis> jedis;
	
	@Inject private ObjectPool<Beanstalkd> beans;
	
	@Inject private UmbrellaConfig umbrella;
	
	@Inject @Named("kernel") private ExecutorService service;
	
	@Override
	public void healthy() {
		LOG.info("service manager starts successfully.");
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
			umbrella.getBeanstalkd().getGroup().shutdownGracefully();
			service.shutdown();
		} catch (Exception dontCare) {}
		LOG.info("service manager stops.");
	}

}
