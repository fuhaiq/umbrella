package com.umbrella.service.netty.json;

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
import com.umbrella.UmbrellaConfig;
import com.umbrella.service.ServiceModule;
import com.umbrella.service.netty.NettyServiceConfig;

public class JsonServiceModule extends ServiceModule {

	public JsonServiceModule(MapBinder<String, Service> serviceBinder) {
		super(serviceBinder);
	}

	@Override
	protected void configure() {
		MapBinder<String, ChannelHandler> handlerBinder = MapBinder.newMapBinder(binder(), String.class, ChannelHandler.class, Names.named("json"));
		handlerBinder.addBinding("decoder").to(JsonDecoder.class).in(Scopes.SINGLETON);
		handlerBinder.addBinding("encoder").to(JsonEncoder.class).in(Scopes.SINGLETON);
		install(new JsonCommandModule(){
			@Override
			protected void configCommand() {
				bindCommand("kernel").to(KernelCommand.class).in(Scopes.SINGLETON);
			}
		});
		handlerBinder.addBinding("json").to(JsonHandler.class).in(Scopes.NO_SCOPE);
		handlerBinder.addBinding("exception").to(JsonExceptionHandler.class).in(Scopes.SINGLETON);
		serviceBinder.addBinding("json").toInstance(new JsonService());
	}
	
	@Provides
	@Singleton
	@Named("json")
	ServerBootstrap provideServerBootstrap(UmbrellaConfig umbrella, @Named("json") Provider<Map<String, ChannelHandler>> handlers) {
		NettyServiceConfig config = umbrella.getService().get("json");
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
