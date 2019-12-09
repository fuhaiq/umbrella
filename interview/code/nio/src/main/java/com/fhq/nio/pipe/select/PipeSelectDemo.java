package com.fhq.nio.pipe.select;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.List;
import java.util.concurrent.ExecutorService;
import com.fhq.nio.socket.server.SelectorProvider;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PipeSelectDemo {

   @Inject
   private SelectorProvider selectorProvider;
   
   @Inject 
   private PipesProvider pipesProvider;
   
   @Inject
   private RunnableFactory runnableFactory;
   
   @Inject
   @Named("pipe")
   private ExecutorService executor;
   
   @Inject
   private MsgHandler handler;
   
   public void show() throws IOException {
      var selector = selectorProvider.get();
      var pipes = pipesProvider.get();
      List<Runnable> threads = Lists.newArrayList();
      
      var num = 0;
      for (Pipe p : pipes) {
         var source = p.source();
         source.configureBlocking(false);
         source.register(selector, source.validOps());
         threads.add(runnableFactory.create(p.sink(), num++));
      }
      threads.forEach(executor::submit);
      while(selector.isOpen()) {
         selector.select(key -> {
            if (key.isReadable()) {
               handler.handleRead(key);
            }
         });
      }
      executor.shutdown();
   }

}
