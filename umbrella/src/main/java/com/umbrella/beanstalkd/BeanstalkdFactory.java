package com.umbrella.beanstalkd;

import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

public class BeanstalkdFactory extends BasePooledObjectFactory<Beanstalkd> {

	private final Logger LOG = LogManager.getLogger("BeanstalkdFactory");
	
	@Inject private BeanstalkdConfig config;

	@Override
	public Beanstalkd create() throws Exception {
		Beanstalkd bean = new Beanstalkd(config.getHost(), config.getPort(), config.getTimeout(), TimeUnit.MILLISECONDS);
		bean.connect(config.getGroup(), config.getChannelClass());
		LOG.info("Create the Beanstalkd [" + bean.toString() + "] to pool");
		return bean;
	}

	@Override
	public void destroyObject(PooledObject<Beanstalkd> p) throws Exception {
		Beanstalkd bean = p.getObject();
		bean.close();
		LOG.info("Destory the Beanstalkd [" + bean.toString() + "] out of pool");
		bean = null;
	}

	@Override
	public boolean validateObject(PooledObject<Beanstalkd> p) {
		Beanstalkd bean = p.getObject();
		return bean.isConnected();
	}

	@Override
	public void passivateObject(PooledObject<Beanstalkd> p) throws Exception {
		Beanstalkd bean = p.getObject();
		LOG.info("Return the Beanstalkd [" + bean.toString() + "] back to pool");
	}

	@Override
	public PooledObject<Beanstalkd> wrap(Beanstalkd obj) {
		return new DefaultPooledObject<Beanstalkd>(obj);
	}
	
}
