package com.fhq.nio.pipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import com.fhq.nio.conf.NioYaml;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class PipeSourceThread implements Callable<Long> {

   @Inject
   private PipeProvider pipeProvider;

   @Inject
   private NioYaml yaml;

   @Override
   public Long call() throws IOException {
      try (var source = pipeProvider.get().source();
            var to = FileChannel.open(Paths.get(yaml.getTo()),
                  Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.CREATE));) {
         var buffer = ByteBuffer.allocate(1024 * 1024);
         while (source.read(buffer) != -1) {
            buffer.flip();
            to.write(buffer);
            buffer.clear();
         }
      }
      long endTime = System.currentTimeMillis();
      return endTime;
   }

}
