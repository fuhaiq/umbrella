package com.umbrella.kernel;

import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wolfram.jlink.KernelLink;

@Component
public class KernelApplicationCleaner {
	
	private final Logger LOG = LoggerFactory.getLogger(KernelApplicationCleaner.class);

	@Autowired
	private ObjectPool<KernelLink> kernelPool;
	
	@Autowired
	private ExecutorService service;
	
	@PreDestroy
	public void destroy() throws Exception {
		kernelPool.clear();
		kernelPool.close();
		LOG.info("销毁Mathematica内核池完成");
		service.shutdown();
		LOG.info("销毁Mathematica内核超时控制器完成");
	}
}
