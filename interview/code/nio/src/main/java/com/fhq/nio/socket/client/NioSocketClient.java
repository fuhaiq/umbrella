package com.fhq.nio.socket.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.fhq.nio.socket.server.SelectorProvider;
import com.google.inject.Inject;

public class NioSocketClient {

   @Inject
   private SelectorProvider selectorProvider;

   @Inject
   private SocketChannelProvider channelProvider;
   
   @Inject
   private ClientHandler handler;
   
   public void disConnect() throws IOException {
      var channel = channelProvider.get();
      var selector = selectorProvider.get();
      if(selector.isOpen()) selector.close();
      if(channel.isOpen()) channel.close();
   }

   public void connect() throws IOException {
      var channel = channelProvider.get();
      var selector = selectorProvider.get();
      channel.register(selector, channel.validOps(), ByteBuffer.allocate(1024));
      while (channel.isConnected()) {
         selector.select(key -> {
            if (key.isReadable()) {
               handler.handleRead(key);
            }
            selector.selectedKeys().remove(key);
         });
      }
   }

}
