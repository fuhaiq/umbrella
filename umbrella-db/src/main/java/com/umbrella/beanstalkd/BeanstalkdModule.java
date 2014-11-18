package com.umbrella.beanstalkd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.alibaba.fastjson.JSON;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class BeanstalkdModule extends AbstractModule{

	private final String config;
	
	public BeanstalkdModule(String config) {
		this.config = config;
	}
	
	@Override
	protected void configure() {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			bind(BeanstalkdConfig.class).toInstance(JSON.parseObject(builder.toString(), BeanstalkdConfig.class));
		} catch (IOException e) {
			addError(e);
		}
		bind(new TypeLiteral<PooledObjectFactory<Beanstalkd>>() {}).to(BeanstalkdFactory.class).in(Scopes.SINGLETON);
	}

	@Provides
	@Singleton
	ObjectPool<Beanstalkd> provideBeanstalkdPool(BeanstalkdConfig beanConfig, PooledObjectFactory<Beanstalkd> beanFactory) {
		GenericObjectPool<Beanstalkd> pool = new GenericObjectPool<Beanstalkd>(beanFactory, beanConfig);
		return pool;
	}
}
