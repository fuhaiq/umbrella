package com.umbrella.redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.umbrella.session.Session;

public class JedisModule extends AbstractModule{

	private final String config;
	
	public JedisModule(String config) {
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
			bind(JedisConfig.class).toInstance(JSON.parseObject(builder.toString(), JedisConfig.class));
		} catch (IOException e) {
			addError(e);
		}
		bind(new TypeLiteral<PooledObjectFactory<Jedis>>() {}).to(JedisFactory.class).in(Scopes.SINGLETON);
		bind(new TypeLiteral<Session<Jedis>>() {}).to(JedisSession.class).in(Scopes.SINGLETON);
		
		JedisInterceptor jedisInterceptor = new JedisInterceptor();
		requestInjection(jedisInterceptor);
		
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(JedisCycle.class), jedisInterceptor);
	}

	@Provides
	@Singleton
	ObjectPool<Jedis> provideJedisPool(JedisConfig jedisConfig, PooledObjectFactory<Jedis> jedisFactory) {
		GenericObjectPool<Jedis> pool = new GenericObjectPool<Jedis>(jedisFactory, jedisConfig);
		return pool;
	}
}
