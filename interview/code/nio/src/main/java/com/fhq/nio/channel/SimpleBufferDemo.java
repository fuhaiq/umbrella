package com.fhq.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;
import com.fhq.nio.conf.NioYaml;
import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SimpleBufferDemo {

   @Inject
   private Provider<ByteSource> provider;

   @Inject
   private NioYaml yaml;

   @Inject
   private Logger logger;

   public void bufferDemo() throws IOException {
      var source = provider.get();

      // 按字节读取
      try (var stream = source.openStream(); var channel = Channels.newChannel(stream);) {
         var buf = ByteBuffer.allocate(1024 * 1024); // 1MB
         int byteRead = -1;
         while ((byteRead = channel.read(buf)) != -1) {
            System.out.println("读取" + byteRead + "字节");
            buf.flip();
            while (buf.hasRemaining()) {
               System.out.print((char) buf.get());
            }
            buf.clear();
         }
      }

      // 按字符读取
      var charSource = source.asCharSource(Charsets.UTF_8);
      try (var reader = charSource.openStream()) {
         var buf = CharBuffer.allocate(1024 * 1024 / 2);// 1MB
         var charRead = -1;
         while ((charRead = reader.read(buf)) != -1) {
            System.out.println("读取" + charRead + "字符");
            buf.flip();
            while (buf.hasRemaining()) {
               System.out.print(buf.get());
            }
            buf.clear();
         }
      }
   }

   public void scatterDemo() throws IOException {
      var header = new StringBuffer();
      var body = new StringBuffer();
      try (var channel = FileChannel.open(Paths.get(yaml.getScatter()), StandardOpenOption.READ)) {
         var headerBuf = ByteBuffer.allocate(6);
         var bodyBuf = ByteBuffer.allocate(1024);
         ByteBuffer[] buffers = {headerBuf, bodyBuf};
         while (channel.read(buffers) != -1) {
            headerBuf.flip();
            while (headerBuf.hasRemaining()) {
               header.append((char) headerBuf.get());
            }
            headerBuf.clear();

            bodyBuf.flip();
            while (bodyBuf.hasRemaining()) {
               body.append((char) bodyBuf.get());
            }
            bodyBuf.clear();
         } ;
         logger.info("消息头为:" + header);
         logger.info("消息体为:" + body);
      }
   }

   public void gatherDemo() throws IOException {
      try (
            var channel = FileChannel.open(Paths.get(yaml.getScatter()), StandardOpenOption.READ);
            var targetChannel = FileChannel.open(Paths.get(yaml.getGather()), StandardOpenOption.WRITE);
            ){
         ByteBuffer[] buffers =
               {ByteBuffer.allocate(4), ByteBuffer.allocate(5), ByteBuffer.allocate(6)};
         while (channel.read(buffers) != -1) {
            for (var i = 0; i < buffers.length; i++) {
               buffers[i].flip();
            }
            targetChannel.write(buffers);
            for (var i = 0; i < buffers.length; i++) {
               buffers[i].clear();
            }
         }
      }
   }
   
   public void transferFromDemo() throws IOException {
      try (
            var source = FileChannel.open(Paths.get(yaml.getRead()), StandardOpenOption.READ);
            var sink = FileChannel.open(Paths.get(yaml.getGather()), StandardOpenOption.WRITE);) {
         long position = 0;
         long count = source.size();
         sink.transferFrom(source, position, count);
      }
   }
   
   public void transferToDemo() throws IOException {
      try (var source = FileChannel.open(Paths.get(yaml.getRead()), StandardOpenOption.READ);
            var sink = FileChannel.open(Paths.get(yaml.getGather()), Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.READ));) {
         long position = 0;
         long count = source.size();
         source.transferTo(position, count, sink);
      }
   }
   
   public long copyFile() throws IOException {
      long startTime = System.currentTimeMillis();
      try (var source = FileChannel.open(Paths.get(yaml.getFrom()), StandardOpenOption.READ);
            var sink = FileChannel.open(Paths.get(yaml.getTo()), Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE));) {
         long position = 0;
         long count = source.size();
         source.transferTo(position, count, sink);
      }
      long endTime = System.currentTimeMillis();
      return endTime - startTime;
   }

   public long copyFIle1() throws IOException {
      long startTime = System.currentTimeMillis();
      try (var source = FileChannel.open(Paths.get(yaml.getFrom()), StandardOpenOption.READ);
            var sink = FileChannel.open(Paths.get(yaml.getTo()), Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE));) {
         var buffer = ByteBuffer.allocate(1024*1024*10); // 1MB
         while(source.read(buffer) != -1) {
            buffer.flip();//read mode
            sink.write(buffer);
            buffer.clear();
         }
      }
      long endTime = System.currentTimeMillis();
      return endTime - startTime;
   }
   
   public long copyFIle2() throws IOException {
      long startTime = System.currentTimeMillis();
      try (var source = FileChannel.open(Paths.get(yaml.getFrom()), StandardOpenOption.READ);
            var sink = FileChannel.open(Paths.get(yaml.getTo()), Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE));) {
         var sourceMapBuffer = source.map(MapMode.READ_ONLY, 0, source.size());
         var sinkMapBuffer = sink.map(MapMode.READ_WRITE, 0, source.size());
         while(sourceMapBuffer.hasRemaining()) {
            sinkMapBuffer.put(sourceMapBuffer.get());
         }
      }
      long endTime = System.currentTimeMillis();
      return endTime - startTime;
   }
}
