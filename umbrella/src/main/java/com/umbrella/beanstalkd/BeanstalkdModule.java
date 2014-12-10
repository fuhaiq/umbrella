package com.umbrella.beanstalkd;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.umbrella.UmbrellaConfig;
import com.umbrella.session.Session;

public class BeanstalkdModule extends AbstractModule{

	@Override
	protected void configure() {
		bind(new TypeLiteral<PooledObjectFactory<Beanstalkd>>() {}).to(BeanstalkdFactory.class).in(Scopes.SINGLETON);
		bind(new TypeLiteral<Session<Beanstalkd>>() {}).to(BeanstalkdSession.class).in(Scopes.SINGLETON);
		
		MethodInterceptor beanstalkdInterceptor = new BeanstalkdInterceptor();
		requestInjection(beanstalkdInterceptor);
		
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(BeanstalkdCycle.class), beanstalkdInterceptor);
	}

	@Provides
	@Singleton
	ObjectPool<Beanstalkd> provideBeanstalkdPool(UmbrellaConfig umbrella, PooledObjectFactory<Beanstalkd> beanFactory) {
		GenericObjectPool<Beanstalkd> pool = new GenericObjectPool<Beanstalkd>(beanFactory, umbrella.getBeanstalkd());
		return pool;
	}
}
