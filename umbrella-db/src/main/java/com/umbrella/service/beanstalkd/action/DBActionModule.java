package com.umbrella.service.beanstalkd.action;

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public abstract class DBActionModule extends AbstractModule {

	private MapBinder<String, DBAction> mapbinder;
	
	@Override
	protected void configure() {
		mapbinder = MapBinder.newMapBinder(binder(), String.class, DBAction.class);
		actionConfigure();
	}

	protected LinkedBindingBuilder<DBAction> mapAction(String key) {
		return mapbinder.addBinding(key);
	}
	
	protected abstract void actionConfigure();
}
