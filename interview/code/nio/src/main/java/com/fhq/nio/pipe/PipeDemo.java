package com.fhq.nio.pipe;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class PipeDemo {

   @Inject
   @Named("pipe")
   private ExecutorService executor;
   
   @Inject
   @Named("source")
   private Callable<Long> source;
   
   @Inject
   @Named("sink")
   private Callable<Long> sink;
   
   public long copyFile() throws InterruptedException, ExecutionException {
      Future<Long> sourceFuture = executor.submit(source);
      Future<Long> sinkFuture = executor.submit(sink);
      long cost = sourceFuture.get().longValue() - sinkFuture.get().longValue();
      executor.shutdown();
      return cost;
   }
}
