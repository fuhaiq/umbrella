package com.fhq.nio.pipe.select;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe.SinkChannel;
import java.util.Random;
import com.fhq.nio.NioException;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class PipeSinkThread implements Runnable {

   private final SinkChannel sink;

   private final int number;

   @Inject
   public PipeSinkThread(@Assisted SinkChannel sink, @Assisted int number) {
      this.sink = sink;
      this.number = number;
   }

   @Override
   public void run() {
      var buffer = ByteBuffer.allocate(1024*1024); // 1MB
      buffer.clear();
      try {
         while (true) {
            var msg = String.format("来自%s管道的消息", number);
            buffer.put(msg.getBytes());
            buffer.flip();
            sink.write(buffer);
            buffer.clear();
            int sleep = new Random().nextInt(5000);
            Thread.sleep(sleep);
         }
      } catch (IOException | InterruptedException e) {
         throw new NioException(e);
      }
   }

}
