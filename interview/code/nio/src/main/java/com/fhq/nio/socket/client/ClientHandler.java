package com.fhq.nio.socket.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import com.fhq.nio.NioException;
import com.google.common.base.Charsets;
import com.google.inject.Inject;

public class ClientHandler {
   
   @Inject
   private Logger logger;

   public void handleRead(SelectionKey key) {
      SocketChannel client = (SocketChannel) key.channel();
      ByteBuffer buffer = (ByteBuffer) key.attachment();
      buffer.clear();
      try {
         var readByte = client.read(buffer); // 将channel信息写入buffer
         if (readByte == -1) client.close();
         buffer.flip();
         logger.info("接收到服务端消息:" + new String(buffer.array(), 0, buffer.limit(), Charsets.UTF_8));
         buffer.clear();
      } catch (IOException e) {
         throw new NioException(e.getMessage(), e);
      }
   }

}
