package com.fhq.nio.pipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import com.fhq.nio.conf.NioYaml;
import com.google.inject.Inject;

public class PipeSinkThread implements Callable<Long> {

   @Inject
   private PipeProvider pipeProvider;

   @Inject
   private NioYaml yaml;

   @Override
   public Long call() throws IOException {
      long startTime = System.currentTimeMillis();
      try (var from = FileChannel.open(Paths.get(yaml.getFrom()), StandardOpenOption.READ);
            var sink = pipeProvider.get().sink();) {
         var buffer = ByteBuffer.allocate(1024 * 1024); // 1MB
         buffer.clear();
         while(from.read(buffer) != -1) {
            buffer.flip();
            sink.write(buffer);
            buffer.clear();
         }
      }
      return startTime;
   }

}
