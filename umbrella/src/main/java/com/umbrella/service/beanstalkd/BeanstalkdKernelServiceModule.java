package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.umbrella.service.ServiceModule;

public class BeanstalkdKernelServiceModule extends ServiceModule {
	
	private final String topicDir;

	public BeanstalkdKernelServiceModule(MapBinder<String, Service> serviceBinder, String topicDir) {
		super(serviceBinder);
		this.topicDir = topicDir;
	}

	@Override
	protected void configure() {
		bind(BeanstalkdKernelManager.class).in(Scopes.SINGLETON);
		bind(String.class).annotatedWith(Names.named("topic.dir")).toInstance(topicDir);
		serviceBinder.addBinding("beanstalkd-kernel").toInstance(new BeanstalkdKernelService());
	}

}
