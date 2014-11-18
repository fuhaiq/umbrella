package com.umbrella.redis;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import com.google.inject.Inject;

public class JedisFactory extends BasePooledObjectFactory<Jedis>{
	
	private final Logger LOG = LogManager.getLogger("JedisFactory");
	
	@Override
	public void destroyObject(PooledObject<Jedis> p) throws Exception {
		Jedis jedis = p.getObject();
		jedis.close();
		LOG.info("Destory the redis connection [" + jedis.toString() + "] out of pool");
		jedis = null;
	}

	@Override
	public boolean validateObject(PooledObject<Jedis> p) {
		Jedis jedis = p.getObject();
		return jedis.isConnected();
	}

	@Override
	public void passivateObject(PooledObject<Jedis> p) throws Exception {
		Jedis jedis = p.getObject();
		LOG.info("Return the redis connection [" + jedis.toString() + "] back to pool");
	}

	@Inject private JedisConfig config;

	@Override
	public Jedis create() throws Exception {
		Jedis jedis = new Jedis(config.getHost(), config.getPort());
		jedis.connect();
		LOG.info("Create the redis connection [" + jedis.toString() + "] to pool");
		return jedis;
	}

	@Override
	public PooledObject<Jedis> wrap(Jedis obj) {
		return new DefaultPooledObject<Jedis>(obj);
	}

}
