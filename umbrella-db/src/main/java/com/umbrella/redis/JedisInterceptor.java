package com.umbrella.redis;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import redis.clients.jedis.Jedis;
import com.google.inject.Inject;
import com.umbrella.session.Session;

public class JedisInterceptor implements MethodInterceptor{

	@Inject private Session<Jedis> session;
	
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
