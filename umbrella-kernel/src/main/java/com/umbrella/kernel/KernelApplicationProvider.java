package com.umbrella.kernel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.umbrella.kernel.link.KernelConfig;
import com.wolfram.jlink.KernelLink;

@Configuration
@PropertySource("classpath:/application.properties")
public class KernelApplicationProvider {
	
	private final Logger LOG = LoggerFactory.getLogger(KernelApplicationProvider.class);

	@Autowired
	@Bean
	public KernelConfig provideKernelConfig(Environment env) {
		KernelConfig config = new KernelConfig();
		config.getLibdir().setName(env.getProperty("kernel.libdir.name"));
		config.getLibdir().setDir(env.getProperty("kernel.libdir.dir"));
		config.setMaxWaitMillis(env.getProperty("kernel.maxWaitMillis", Long.class));
		config.setTimeConstrained(env.getProperty("kernel.timeConstrained", Integer.class));
		config.setMaxTotal(env.getProperty("kernel.maxTotal", Integer.class));
		config.setUrl(env.getProperty("kernel.url"));
		config.setImgDir(env.getProperty("kernel.imgDir"));
		config.setJmxEnabled(false);
		return config;
	}
	
	@Autowired
	@Bean
	public ObjectPool<KernelLink> provideKernelLinkPool(KernelConfig kernelConfig, PooledObjectFactory<KernelLink> kernelFactory) {
		System.setProperty(kernelConfig.getLibdir().getName(), kernelConfig.getLibdir().getDir());
		GenericObjectPool<KernelLink> pool = new GenericObjectPool<KernelLink>(kernelFactory, kernelConfig);
		LOG.info("初始化Mathematica内核池完成");
		return pool;
	}
	
	@Autowired
	@Bean
	public ExecutorService provideKernelTimeoutExecutorService(KernelConfig kernelConfig) {
		ExecutorService service = Executors.newFixedThreadPool(kernelConfig.getMaxTotal(), new ThreadFactoryBuilder().setNameFormat("kernel-timeout-thread").build());
		LOG.info("初始化Mathematica计算超时控制器完成");
		return service;
	}
	
}
