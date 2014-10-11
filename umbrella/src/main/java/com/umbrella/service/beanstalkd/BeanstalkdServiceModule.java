package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.service.ServiceModule;

public class BeanstalkdServiceModule extends ServiceModule {

	public BeanstalkdServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		serviceBinder.addBinding("beanstalkd-1").toInstance(new BeanstalkdService());
		serviceBinder.addBinding("beanstalkd-2").toInstance(new BeanstalkdService());
	}

}
