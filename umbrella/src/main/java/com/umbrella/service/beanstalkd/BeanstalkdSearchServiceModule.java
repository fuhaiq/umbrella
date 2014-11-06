package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.service.ServiceModule;

public class BeanstalkdSearchServiceModule extends ServiceModule {

	private final String url;
	
	private final int port;
	
	public BeanstalkdSearchServiceModule(MapBinder<String, Service> serviceBinder, String url, int port) {
		super(serviceBinder);
		this.url = url;
		this.port = port;
	}

	@Override
	protected void configure() {
		serviceBinder.addBinding("beanstalkd-search").toInstance(new BeanstalkdSearchService(url, port));
	}

}
