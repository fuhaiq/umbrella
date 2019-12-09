package com.fhq.nio.pipe.select;

import java.nio.channels.Pipe.SourceChannel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.logging.Logger;
import com.fhq.nio.NioException;
import com.google.common.base.Charsets;
import com.google.inject.Inject;

public class MsgHandler {

   @Inject
   private Logger logger;

   public void handleRead(SelectionKey key) {
      SourceChannel channel = (SourceChannel) key.channel();
      var buffer = ByteBuffer.allocate(1024*1024); // 1MB
      try {
         while (channel.read(buffer) > 0) {
            buffer.flip();
            logger.info("接收到消息:" + new String(buffer.array(), 0, buffer.limit(), Charsets.UTF_8));
            buffer.clear();
         }
      } catch (IOException e) {
         throw new NioException(e.getMessage(), e);
      }
   }
}
