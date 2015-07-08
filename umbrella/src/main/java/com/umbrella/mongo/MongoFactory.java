package com.umbrella.mongo;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.umbrella.UmbrellaConfig;

public class MongoFactory extends BasePooledObjectFactory<MongoClient>{
	
	private final Logger LOG = LogManager.getLogger("MongoFactory");
	
	@Inject private UmbrellaConfig umbrella;
	
	@Override
	public void destroyObject(PooledObject<MongoClient> p) throws Exception {
		MongoClient mongo = p.getObject();
		mongo.close();
		LOG.info("Destory the mongo connection [" + mongo.toString() + "] out of pool");
		mongo = null;
	}

	@Override
	public void passivateObject(PooledObject<MongoClient> p) throws Exception {
		MongoClient mongo = p.getObject();
		LOG.info("Return the mongo connection [" + mongo.toString() + "] back to pool");
	}

	@Override
	public MongoClient create() throws Exception {
		MongoConfig config = umbrella.getMongo();
		MongoClient mongoClient = new MongoClient(config.getHost(), config.getPort());
		LOG.info("Create the mongo connection [" + mongoClient.toString() + "] to pool");
		return mongoClient;
	}

	@Override
	public PooledObject<MongoClient> wrap(MongoClient obj) {
		return new DefaultPooledObject<MongoClient>(obj);
	}

}
