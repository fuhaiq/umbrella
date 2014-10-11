package com.umbrella.service.kernel.action;

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public abstract class JsonActionModule extends AbstractModule{

	private MapBinder<String, JsonAction> mapbinder;
	
	@Override
	protected void configure() {
		mapbinder = MapBinder.newMapBinder(binder(), String.class, JsonAction.class);
		actionConfigure();
	}

	protected LinkedBindingBuilder<JsonAction> mapAction(String key) {
		return mapbinder.addBinding(key);
	}
	
	protected abstract void actionConfigure();
}
