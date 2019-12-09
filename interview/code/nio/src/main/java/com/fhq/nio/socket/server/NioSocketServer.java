package com.fhq.nio.socket.server;

import java.io.IOException;
import com.google.inject.Inject;

public class NioSocketServer {

   @Inject
   private ServerSocketChannelProvider channelProvider;

   @Inject
   private SelectorProvider selectorProvider;

   @Inject
   private ServerHandler handler;

   public void stop() throws IOException {
      var channel = channelProvider.get();
      var selector = selectorProvider.get();
      if(selector.isOpen()) selector.close();
      if(channel.isOpen()) channel.close();
   }

   public void start() throws IOException {
      var channel = channelProvider.get();
      var selector = selectorProvider.get();
      // Server-socket channels only support the accepting of new connections
      channel.register(selector, channel.validOps());
      while (channel.isOpen()) {
         selector.select(key -> {
            if (key.isAcceptable())
               handler.handleAccept(key);
            if (key.isReadable())
               handler.handleRead(key);
            selector.selectedKeys().remove(key);
         });
      }
   }

}
