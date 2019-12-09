package com.fhq.nio.socket.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;
import com.fhq.nio.NioException;
import com.google.common.base.Charsets;
import com.google.inject.Inject;

public class ServerHandler {

   @Inject
   private Logger logger;

   public void handleAccept(SelectionKey key) {
      ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
      // 获取客户端链接，并注册到Selector中
      SocketChannel client;
      try {
         client = serverSocketChannel.accept();
         client.configureBlocking(false);
         // 注册通道到Selector，然后设置监听事件，第三个参数是将你需要带的东西，一般是buffer或消息体
         var buffer = ByteBuffer.allocate(1024);
         client.register(key.selector(), client.validOps(), buffer);
         logger.info("完成客户端注册");
         var welcome = "欢迎光临";
         buffer.put(welcome.getBytes(Charsets.UTF_8));
         buffer.flip();
         client.write(buffer);
         buffer.clear();
      } catch (IOException e) {
         throw new NioException(e.getMessage(), e);
      }
   }

   public void handleRead(SelectionKey key) {
      SocketChannel client = (SocketChannel) key.channel();
      ByteBuffer buffer = (ByteBuffer) key.attachment(); // 服务端绑定的客户端buffer
      buffer.clear(); // 清空上次的消息
      try {
         var readByte = client.read(buffer); // 将channel信息写入buffer
         if(readByte == -1) client.close();
         buffer.flip();
         logger.info("接收到客户端消息:" + new String(buffer.array(), 0, buffer.limit(), Charsets.UTF_8));
         buffer.clear();
      } catch (IOException e) {
         throw new NioException(e.getMessage(), e);
      }
   }

}
