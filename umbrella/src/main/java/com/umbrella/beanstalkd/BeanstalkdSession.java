package com.umbrella.beanstalkd;

import org.apache.commons.pool2.ObjectPool;

import com.google.inject.Inject;
import com.umbrella.session.AbstractSession;

public class BeanstalkdSession extends AbstractSession<Beanstalkd> {
	
	@Inject private ObjectPool<Beanstalkd> pool;

	@Override
	protected Beanstalkd makeObject() throws Exception {
		return pool.borrowObject();
	}

	@Override
	protected void returnObject(Beanstalkd t) throws Exception {
		pool.returnObject(t);
	}

}
