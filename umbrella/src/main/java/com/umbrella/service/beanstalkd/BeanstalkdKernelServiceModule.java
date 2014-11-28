package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.service.ServiceModule;

public class BeanstalkdKernelServiceModule extends ServiceModule {
	
	public BeanstalkdKernelServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		bind(BeanstalkdKernelManager.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-kernel").toInstance(new BeanstalkdKernelService());
	}

}
