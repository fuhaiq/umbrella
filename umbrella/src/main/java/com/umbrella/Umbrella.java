package com.umbrella;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Umbrella {

	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new ServiceManagerModule());
		ServiceManager manager = injector.getInstance(ServiceManager.class);
		manager.startAsync().awaitHealthy();
	}

}
