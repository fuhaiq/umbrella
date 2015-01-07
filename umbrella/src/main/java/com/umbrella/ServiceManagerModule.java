package com.umbrella;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.umbrella.kernel.KernelModule;
import com.umbrella.service.netty.kernel.KernelServiceModule;
import com.umbrella.service.netty.telnet.TelnetServiceModule;

public class ServiceManagerModule extends AbstractModule{
	
	private MapBinder<String, Service> serviceBinder;
	
	private final String config;
	
	public ServiceManagerModule(String config) {
		this.config = config;
	}
	
	@Override
	protected void configure() {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(config);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			bind(UmbrellaConfig.class).toInstance(JSON.parseObject(builder.toString(), UmbrellaConfig.class));
		} catch (IOException e) {
			addError(e);
		}
		
		serviceBinder = MapBinder.newMapBinder(binder(), String.class, Service.class);
		install(new KernelModule());
		install(new TelnetServiceModule(serviceBinder));
		install(new KernelServiceModule(serviceBinder));
		
		Multibinder<ServiceManager.Listener> listenerBinder = Multibinder.newSetBinder(binder(), ServiceManager.Listener.class);
		listenerBinder.addBinding().to(ServiceManagerListener.class).in(Scopes.SINGLETON);
	}
	
	@Provides
	@Singleton
	ServiceManager provideServiceManager(Set<ServiceManager.Listener> listeners, Map<String, Service> serviceMapping) {
		ServiceManager manager = new ServiceManager(serviceMapping.values());
		listeners.stream().forEach(r->manager.addListener(r));
		return manager;
	}
	
}
