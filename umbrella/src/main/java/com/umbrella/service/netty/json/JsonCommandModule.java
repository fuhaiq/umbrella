package com.umbrella.service.netty.json;

import com.google.inject.AbstractModule;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public class JsonCommandModule extends AbstractModule {
	
	private MapBinder<String, JsonCommand> commandBinder;

	@Override
	protected final void configure() {
		commandBinder = MapBinder.newMapBinder(binder(), String.class, JsonCommand.class);
		configCommand();
	}

	protected void configCommand(){}
	
	protected final LinkedBindingBuilder<JsonCommand> bindCommand(String key) {
		return commandBinder.addBinding(key);
	}
}
