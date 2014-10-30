package com.umbrella;

import javax.sql.DataSource;

import org.apache.commons.pool2.ObjectPool;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mchange.v2.c3p0.DataSources;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.beanstalkd.BeanstalkdConfig;

public class ServiceManagerListener extends ServiceManager.Listener{

	private final Logger LOG = LogManager.getLogger(ServiceManagerListener.class);
	
	@Inject
	private ObjectPool<Beanstalkd> beans;
	
	@Inject
	private SqlSessionFactory factory;
	
	@Inject
	private Provider<ServiceManager> manager;
	
	@Inject
	private BeanstalkdConfig beansConfig;
	
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
		DataSource ds = factory.getConfiguration().getEnvironment().getDataSource();
		try {
			DataSources.destroy(ds);
			beans.clear();
			beans.close();
			beansConfig.getGroup().shutdownGracefully();
		} catch (Exception dontCare) {}
		LOG.info("service manager stops.");
	}

}
