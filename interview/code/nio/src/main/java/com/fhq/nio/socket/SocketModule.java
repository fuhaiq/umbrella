package com.fhq.nio.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import com.fhq.nio.conf.NioYaml;
import com.fhq.nio.conf.Server;
import com.fhq.nio.socket.client.ClientHandler;
import com.fhq.nio.socket.client.NioSocketClient;
import com.fhq.nio.socket.client.SocketChannelProvider;
import com.fhq.nio.socket.server.NioSocketServer;
import com.fhq.nio.socket.server.SelectorProvider;
import com.fhq.nio.socket.server.ServerHandler;
import com.fhq.nio.socket.server.ServerSocketChannelProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class SocketModule extends AbstractModule {
   
   @Override
   protected void configure() {
      install(ThrowingProviderBinder.forModule(this));
      bind(ServerHandler.class).in(Scopes.SINGLETON);
      bind(NioSocketServer.class).in(Scopes.SINGLETON);
      
      bind(ClientHandler.class).in(Scopes.SINGLETON);
      bind(NioSocketClient.class).in(Scopes.SINGLETON);
   }
   
   @CheckedProvides(SelectorProvider.class)
   @Singleton
   Selector provideSelector() throws IOException {
      return Selector.open();
   }
   
   @CheckedProvides(ServerSocketChannelProvider.class)
   @Singleton
   ServerSocketChannel provideServerSocketChannel(NioYaml yaml) throws IOException {
      Server server = yaml.getSocket().getServer();
      ServerSocketChannel channel = ServerSocketChannel.open();
      channel.configureBlocking(false);
      channel.bind(new InetSocketAddress(server.getHost(), server.getPort()));
      return channel;
   }
   
   @CheckedProvides(SocketChannelProvider.class)
   @Singleton
   SocketChannel provideSocketChannel(NioYaml yaml) throws IOException {
      Server server = yaml.getSocket().getServer();
      SocketChannel channel = SocketChannel.open();
      channel.connect(new InetSocketAddress(server.getHost(), server.getPort()));
      channel.configureBlocking(false);
      return channel;
   }

}
