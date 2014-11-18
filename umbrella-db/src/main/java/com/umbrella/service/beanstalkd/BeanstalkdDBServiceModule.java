package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.service.ServiceModule;
import com.umbrella.service.beanstalkd.action.DBActionModule;
import com.umbrella.service.beanstalkd.action.topic.AddResult;
import com.umbrella.service.beanstalkd.action.topic.Create;

public class BeanstalkdDBServiceModule extends ServiceModule {

	public BeanstalkdDBServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		install(new DBActionModule() {
			@Override
			protected void actionConfigure() {
				mapAction("topic.insert").to(Create.class).in(Scopes.SINGLETON);
				mapAction("topic.add.result").to(AddResult.class).in(Scopes.SINGLETON);
			}
		});
		serviceBinder.addBinding("beanstalkd-db").to(BeanstalkdDBService.class).in(Scopes.SINGLETON);
	}

}
