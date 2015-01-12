package com.umbrella.redis;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

import redis.clients.jedis.Jedis;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.umbrella.UmbrellaConfig;
import com.umbrella.session.Session;

public class JedisModule extends AbstractModule{
	
	@Override
	protected void configure() {
		bind(new TypeLiteral<PooledObjectFactory<Jedis>>() {}).to(JedisFactory.class).in(Scopes.SINGLETON);
		bind(new TypeLiteral<Session<Jedis>>() {}).to(JedisSession.class).in(Scopes.SINGLETON);
		
		JedisInterceptor jedisInterceptor = new JedisInterceptor();
		requestInjection(jedisInterceptor);
		
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(JedisCycle.class), jedisInterceptor);
	}

	@Provides
	@Singleton
	ObjectPool<Jedis> provideJedisPool(UmbrellaConfig umbrella, PooledObjectFactory<Jedis> jedisFactory) {
		GenericObjectPool<Jedis> pool = new GenericObjectPool<Jedis>(jedisFactory, umbrella.getRedis());
		return pool;
	}
}
