package com.umbrella.kernel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.umbrella.kernel.link.KernelConfig;
import com.wolfram.jlink.KernelLink;

@Configuration
@PropertySource("classpath:/application.properties")
public class KernelConfiguration {
	
	private @Value("${kernel.libdir.name}") String libDirName;
	
	private @Value("${kernel.libdir.dir}") String libDir;
	
	private @Value("${kernel.maxWaitMillis}") long maxWaitMillis;
	
	private @Value("${kernel.timeConstrained}") int timeConstrained;
	
	private @Value("${kernel.maxTotal}") int maxTotal;
	
	private @Value("${kernel.url}") String url;
	
	private @Value("${kernel.imgDir}") String imgDir;
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
	@Autowired
	@Bean
	public KernelConfig provideKernelConfig(Environment env) {
		KernelConfig config = new KernelConfig();
		config.getLibdir().setName(libDirName);
		config.getLibdir().setDir(libDir);
		config.setMaxWaitMillis(maxWaitMillis);
		config.setTimeConstrained(timeConstrained);
		config.setMaxTotal(maxTotal);
		config.setUrl(url);
		config.setImgDir(imgDir);
		config.setJmxEnabled(false);
		return config;
	}
	
	@Autowired
	@Bean(destroyMethod = "close")
	public ObjectPool<KernelLink> provideKernelLinkPool(KernelConfig kernelConfig, PooledObjectFactory<KernelLink> kernelFactory) {
		System.setProperty(kernelConfig.getLibdir().getName(), kernelConfig.getLibdir().getDir());
		GenericObjectPool<KernelLink> pool = new GenericObjectPool<KernelLink>(kernelFactory, kernelConfig);
		return pool;
	}
	
	@Autowired
	@Bean(destroyMethod = "shutdown")
	public ExecutorService provideKernelTimeoutExecutorService(KernelConfig kernelConfig) {
		ExecutorService service = Executors.newFixedThreadPool(kernelConfig.getMaxTotal(), new ThreadFactoryBuilder().setNameFormat("kernel-timeout-thread").build());
		return service;
	}
	
}
