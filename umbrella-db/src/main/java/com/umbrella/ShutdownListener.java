package com.umbrella;

import javax.sql.DataSource;

import org.apache.commons.pool2.ObjectPool;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.mchange.v2.c3p0.DataSources;
import com.umbrella.beanstalkd.Beanstalkd;
import com.umbrella.beanstalkd.BeanstalkdConfig;

public class ShutdownListener extends ServiceManager.Listener{

	private final Logger LOG = LogManager.getLogger("ServiceManager");
	
	@Inject
	private ObjectPool<Beanstalkd> beans;
	
	@Inject
	private SqlSessionFactory factory;
	
	@Inject
	private BeanstalkdConfig beansConfig;
	
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
