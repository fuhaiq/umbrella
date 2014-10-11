package com.umbrella.kernel;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.umbrella.session.Session;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;

public class KernelInterceptor implements MethodInterceptor{

	@Inject private Session<KernelLink> session;
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if(!session.isClosed()) return invocation.proceed();
		try {
			session.start();
			Object obj = invocation.proceed();
			session.get().newPacket();
			return obj;
		} catch (MathLinkException e) {
			KernelLink kernelLink = session.get();
			kernelLink.clearError();
			kernelLink.newPacket();
			throw e;
		} catch (Throwable e) {
			throw e;
		} finally {
			if(!session.isClosed()) {
				session.close();
			}
		}
	}
}