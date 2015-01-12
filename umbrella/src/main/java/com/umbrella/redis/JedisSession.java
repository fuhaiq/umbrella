package com.umbrella.redis;

import org.apache.commons.pool2.ObjectPool;

import redis.clients.jedis.Jedis;

import com.google.inject.Inject;
import com.umbrella.session.AbstractSession;

public class JedisSession extends AbstractSession<Jedis>{

	@Inject
	private ObjectPool<Jedis> pool;
	
	@Override
	protected Jedis makeObject() throws Exception {
		return pool.borrowObject();
	}

	@Override
	protected void returnObject(Jedis t) throws Exception {
		pool.returnObject(t);
	}

}
