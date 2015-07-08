package com.umbrella;

import java.util.concurrent.ExecutorService;

import org.apache.commons.pool2.ObjectPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import com.umbrella.beanstalkd.Beanstalkd;
import com.wolfram.jlink.KernelLink;

import redis.clients.jedis.Jedis;

public class ServiceManagerListener extends ServiceManager.Listener{

	private final Logger LOG = LogManager.getLogger("service-manager");
	
	@Inject private ObjectPool<KernelLink> kernel;
	
	@Inject private ObjectPool<Beanstalkd> beans;
	
	@Inject private ObjectPool<Jedis> jedis;
	
	@Inject private ObjectPool<MongoClient> mongo;
	
	@Inject @Named("kernel") private ExecutorService service;
	
	@Inject private UmbrellaConfig umbrella;
	
	@Override
	public void healthy() {
		LOG.info("All services start");
	}
	
	@Override
	public void stopped() {
		try {
			kernel.clear();
			kernel.close();
			beans.clear();
			beans.close();
			jedis.clear();
			jedis.close();
			mongo.clear();
			mongo.close();
			umbrella.getBeanstalkd().getGroup().shutdownGracefully();
			service.shutdown();
		} catch (Exception dontCare) {}
		LOG.info("All services shutdown");
	}

}
