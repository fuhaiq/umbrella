package com.umbrella;

import java.util.Map;
import java.util.Set;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.umbrella.beanstalkd.BeanstalkdModule;
import com.umbrella.db.DBModule;
import com.umbrella.kernel.KernelModule;
import com.umbrella.mail.MailModule;
import com.umbrella.redis.JedisModule;
import com.umbrella.service.ServiceConfig;
import com.umbrella.service.ServiceType;
import com.umbrella.service.beanstalkd.BeanstalkdKernelServiceModule;
import com.umbrella.service.beanstalkd.BeanstalkdMailServiceModule;
import com.umbrella.service.kernel.KernelServiceModule;
import com.umbrella.service.telnet.TelnetServiceModule;

public class ServiceManagerModule extends AbstractModule{
	
	private MapBinder<String, Service> serviceBinder;
	
	@Override
	protected void configure() {
		serviceBinder = MapBinder.newMapBinder(binder(), String.class, Service.class);
		install(new MailModule("mail.json"));
		install(new DBModule("db.xml"));
		install(new JedisModule("redis.json"));
		install(new BeanstalkdModule("beanstalkd.json"));
		install(new KernelModule("kernel.json"));
		install(new TelnetServiceModule(serviceBinder, new ServiceConfig("localhost", 8000, new ServiceType.EPOLL())));
		install(new KernelServiceModule(serviceBinder, new ServiceConfig("localhost", 8001, new ServiceType.EPOLL())));
		install(new BeanstalkdKernelServiceModule(serviceBinder));
		install(new BeanstalkdMailServiceModule(serviceBinder));
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
