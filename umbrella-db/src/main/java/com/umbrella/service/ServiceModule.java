package com.umbrella.service;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.common.base.Preconditions;

public abstract class ServiceModule extends AbstractModule {

	protected final MapBinder<String, Service> serviceBinder;

	public ServiceModule(MapBinder<String, Service> serviceBinder) {
		this.serviceBinder = Preconditions.checkNotNull(serviceBinder, "service binder is null");
	}

}
