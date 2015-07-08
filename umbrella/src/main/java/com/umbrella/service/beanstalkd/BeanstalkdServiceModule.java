package com.umbrella.service.beanstalkd;

import com.google.common.util.concurrent.Service;
import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import com.umbrella.kit.MailKit;
import com.umbrella.kit.ReplyKit;
import com.umbrella.kit.TopicKit;
import com.umbrella.service.ServiceModule;

public class BeanstalkdServiceModule extends ServiceModule {
	
	public BeanstalkdServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		bind(TopicKit.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-topic").toInstance(new BeanstalkdTopicService());
		bind(MailKit.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-mail").toInstance(new BeanstalkdMailService());
		bind(ReplyKit.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("beanstalkd-reply").toInstance(new BeanstalkdReplyService());
	}

}
