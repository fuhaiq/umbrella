package com.umbrella.service.telnet;

import io.netty.bootstrap.ServerBootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.umbrella.UmbrellaConfig;
import com.umbrella.service.ServiceConfig;

public class TelnetService extends AbstractIdleService{
	
	private final Logger LOG = LogManager.getLogger("telnet-service");
	
	@Inject @Named("telnet")
	private Provider<ServerBootstrap> boot;
	
	@Inject private UmbrellaConfig umbrella;
	
	@Override
	protected void shutDown() throws Exception {
		ServiceConfig config = umbrella.getService().get("telnet");
		config.getWorker().shutdownGracefully();
		config.getBoss().shutdownGracefully();
		LOG.info("telnet service stops");
	}

	@Override
	protected void startUp() {
		ServiceConfig config = umbrella.getService().get("telnet");
		boot.get().bind(config.getHost(), config.getPort()).addListener(r->{
			if(!r.isSuccess()) {
				stopAsync();
				LOG.error("telnet service started failed at port:" + config.getPort());
				throw new Exception(r.cause());
			} else {
				LOG.info("telnet service started at port:" + config.getPort());
			}
		});
	}

}
