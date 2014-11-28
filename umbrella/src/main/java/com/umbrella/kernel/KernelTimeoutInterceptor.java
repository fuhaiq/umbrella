package com.umbrella.kernel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.umbrella.session.Session;
import com.wolfram.jlink.KernelLink;

public class KernelTimeoutInterceptor implements MethodInterceptor{
	
	@Inject private Session<KernelLink> session;
	
	@Inject private KernelConfig kernelConfig;
	
	private final int additional = 3;
	
	@Inject @Named("kernel") private ExecutorService service;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		final KernelLink kernel = session.get();
		CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
		service.execute(()->{
			try {
				future.get(kernelConfig.getTimeConstrained() + additional, TimeUnit.SECONDS);
			} catch(ExecutionException dontCare) {
			} catch (InterruptedException | TimeoutException e) {
				kernel.abandonEvaluation();
			}
		});
		Object obj = null;
		try {
			obj = invocation.proceed();
			future.complete(true);
		} catch (Throwable e) {
			future.completeExceptionally(e);
			throw e;
		}
		return obj;
	}

}
