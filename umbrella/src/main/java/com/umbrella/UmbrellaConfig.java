package com.umbrella;

import java.util.Map;

import com.umbrella.kernel.KernelConfig;
import com.umbrella.service.netty.NettyServiceConfig;

public class UmbrellaConfig {
	
	private KernelConfig kernel;
	
	private Map<String, NettyServiceConfig> service;
	
	public KernelConfig getKernel() {
		return kernel;
	}

	public void setKernel(KernelConfig kernel) {
		this.kernel = kernel;
	}

	public Map<String, NettyServiceConfig> getService() {
		return service;
	}

	public void setService(Map<String, NettyServiceConfig> service) {
		this.service = service;
	}
}
