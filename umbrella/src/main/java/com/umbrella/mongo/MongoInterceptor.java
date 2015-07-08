package com.umbrella.mongo;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.mongodb.MongoClient;
import com.umbrella.session.Session;

public class MongoInterceptor implements MethodInterceptor{

	@Inject private Session<MongoClient> session;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if(!session.isClosed()) return invocation.proceed();
		try {
			session.start();
			return invocation.proceed();
		} catch (Throwable e) {
			throw e;
		} finally {
			if(!session.isClosed()) {
				session.close();
			}
		}
	}

}
