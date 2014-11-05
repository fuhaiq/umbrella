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

	private final Logger LOG = LogManager.getLogger(KernelService.class);
	
	@Inject @Named("kernel")
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
	protected void startUp() throws Exception {
		boot.get().bind(config.getHost(), config.getPort()).addListener(r->{
			if(!r.isSuccess()) {
				stopAsync();
				LOG.error("kernel service started failed at port:" + config.getPort());
				throw new Exception(r.cause());
			} else {
				LOG.info("kernel service started at port:" + config.getPort());
			}
		});
	}

}
