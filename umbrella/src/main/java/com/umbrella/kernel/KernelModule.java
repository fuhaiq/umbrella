package com.umbrella.kernel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.umbrella.UmbrellaConfig;
import com.umbrella.session.Session;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.PacketListener;

public class KernelModule extends AbstractModule{

	@Override
	protected void configure() {
		bind(PacketListener.class).to(KernelListener.class).in(Scopes.SINGLETON);
		bind(new TypeLiteral<PooledObjectFactory<KernelLink>>() {}).to(KernelLinkFactory.class).in(Scopes.SINGLETON);
		bind(new TypeLiteral<Session<KernelLink>>() {}).to(KernelSession.class).in(Scopes.SINGLETON);
		
		MethodInterceptor kernelInterceptor = new KernelInterceptor();
		requestInjection(kernelInterceptor);
		
		MethodInterceptor kernelTimeoutInterceptor = new KernelTimeoutInterceptor();
		requestInjection(kernelTimeoutInterceptor);
		
		bindInterceptor(Matchers.any(), Matchers.annotatedWith(KernelCycle.class), kernelInterceptor, kernelTimeoutInterceptor);
		bind(Kernel.class).to(KernelImpl.class).in(Scopes.SINGLETON);
		
	}

	@Provides
	@Singleton
	ObjectPool<KernelLink> provideKernelLinkPool(UmbrellaConfig umbrella, PooledObjectFactory<KernelLink> kernelFactory) {
		KernelConfig kernelConfig = umbrella.getKernel();
		System.setProperty(kernelConfig.getLibdir().getName(), kernelConfig.getLibdir().getDir());
		GenericObjectPool<KernelLink> pool = new GenericObjectPool<KernelLink>(kernelFactory, kernelConfig);
		return pool;
	}
	
	@Provides
	@Named("kernel")
	@Singleton
	ExecutorService provideKernelTimeoutExecutorService(UmbrellaConfig umbrella) {
		return Executors.newFixedThreadPool(umbrella.getKernel().getMaxTotal(), new ThreadFactoryBuilder().setNameFormat("kernel-timeout-thread").build());
	}
}
