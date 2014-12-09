package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.service.ServiceModule;

public class BeanstalkdMailServiceModule extends ServiceModule {

	public BeanstalkdMailServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		bind(BeanstalkdMailManager.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-mail").toInstance(new BeanstalkdMailService());
	}

}
