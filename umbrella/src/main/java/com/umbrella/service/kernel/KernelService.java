package com.umbrella.service.kernel;

import io.netty.bootstrap.ServerBootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.umbrella.service.RpcServiceConfig;

public class KernelService extends AbstractIdleService{

	private final Logger LOG = LogManager.getLogger(KernelServiceModule.ID);
	
	@Inject @Named(KernelServiceModule.ID)
	private Provider<ServerBootstrap> boot;
	
	private final RpcServiceConfig config;
	
	public KernelService(RpcServiceConfig config) {
		this.config = config;
	}
	
	@Override
	protected void shutDown() throws Exception {
		config.getWorker().shutdownGracefully();
		config.getBoss().shutdownGracefully();
		LOG.info("kernel service stops");
	}

	@Override
	protected void startUp() {
		boot.get().bind(config.getHost(), config.getPort()).addListener(r->{
			if(!r.isSuccess()) {
				stopAsync();
				LOG.error("kernel started failed at port:" + config.getPort());
			} else {
				LOG.info("kernel started at port:" + config.getPort());
			}
		});
	}

}
