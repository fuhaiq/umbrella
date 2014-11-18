package com.umbrella.beanstalkd;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.umbrella.session.Session;

public class BeanstalkdInterceptor implements MethodInterceptor{
	
	@Inject private Session<Beanstalkd> session;

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
