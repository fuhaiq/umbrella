package com.umbrella.mongo;

import org.apache.commons.pool2.ObjectPool;

import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.umbrella.session.AbstractSession;

public class MongoSession extends AbstractSession<MongoClient>{

	@Inject
	private ObjectPool<MongoClient> pool;
	
	@Override
	protected MongoClient makeObject() throws Exception {
		return pool.borrowObject();
	}

	@Override
	protected void returnObject(MongoClient t) throws Exception {
		pool.returnObject(t);
	}

}
