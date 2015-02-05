package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.service.ServiceModule;
import com.umbrella.service.beanstalkd.kernel.BeanstalkdTopicManager;
import com.umbrella.service.beanstalkd.kernel.BeanstalkdTopicService;
import com.umbrella.service.beanstalkd.mail.BeanstalkdMailManager;
import com.umbrella.service.beanstalkd.mail.BeanstalkdMailService;

public class BeanstalkdServiceModule extends ServiceModule {
	
	public BeanstalkdServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		bind(BeanstalkdTopicManager.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-topic").toInstance(new BeanstalkdTopicService());
		bind(BeanstalkdMailManager.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-mail").toInstance(new BeanstalkdMailService());
	}

}
