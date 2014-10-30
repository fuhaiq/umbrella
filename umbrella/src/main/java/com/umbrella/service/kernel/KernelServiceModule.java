package com.umbrella.service.kernel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import java.util.Map;

import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.umbrella.service.RpcServiceConfig;
import com.umbrella.service.ServiceModule;
import com.umbrella.service.kernel.action.Evaluate;
import com.umbrella.service.kernel.action.JsonActionHandler;
import com.umbrella.service.kernel.action.JsonActionModule;

public final class KernelServiceModule extends ServiceModule{
	
	public KernelServiceModule(MapBinder<String, Service> serviceBinder, RpcServiceConfig config) {
		super(serviceBinder);
		this.config = config;
	}

	private final RpcServiceConfig config;
	
	@Override
	protected void configure() {
		install(new JsonActionModule(){
			@Override
			protected void actionConfigure() {
				mapAction("evaluate").to(Evaluate.class).in(Scopes.SINGLETON);
			}
		});
		MapBinder<String, ChannelHandler> mapbinder = MapBinder.newMapBinder(binder(), String.class, ChannelHandler.class, Names.named("kernel"));
		mapbinder.addBinding("decoder").to(JsonDecoder.class).in(Scopes.SINGLETON);
		mapbinder.addBinding("encoder").to(JsonEncoder.class).in(Scopes.SINGLETON);
		mapbinder.addBinding("action").to(JsonActionHandler.class).in(Scopes.NO_SCOPE);
		mapbinder.addBinding("exception").to(JsonExceptionHandler.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("kernel").toInstance(new KernelService(config));
	}
	
	@Provides
	@Singleton
	@Named("kernel")
	ServerBootstrap provideServerBootstrap(@Named("kernel") Provider<Map<String, ChannelHandler>> handlers) {
		ServerBootstrap boot = new ServerBootstrap();
		boot.group(config.getBoss(), config.getWorker()).channel(config.getChannelClass()).childHandler(new ChannelInitializer<SocketChannel>(){
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						handlers.get().forEach((key,handler) -> p.addLast(key, handler));
					}});
		return boot;
	}

}
