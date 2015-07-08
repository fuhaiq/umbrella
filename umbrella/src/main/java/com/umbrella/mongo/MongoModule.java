package com.umbrella.mongo;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.mongodb.MongoClient;
import com.umbrella.UmbrellaConfig;
import com.umbrella.session.Session;

public class MongoModule extends AbstractModule{
	
	@Override
	protected void configure() {
		bind(new TypeLiteral<PooledObjectFactory<MongoClient>>() {}).to(MongoFactory.class).in(Scopes.SINGLETON);
		bind(new TypeLiteral<Session<MongoClient>>() {}).to(MongoSession.class).in(Scopes.SINGLETON);
		
		MongoInterceptor mongoInterceptor = new MongoInterceptor();
		requestInjection(mongoInterceptor);
		
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(MongoCycle.class), mongoInterceptor);
	}

	@Provides
	@Singleton
	ObjectPool<MongoClient> provideMongoPool(UmbrellaConfig umbrella, PooledObjectFactory<MongoClient> mongoFactory) {
		GenericObjectPool<MongoClient> pool = new GenericObjectPool<MongoClient>(mongoFactory, umbrella.getMongo());
		return pool;
	}
}
