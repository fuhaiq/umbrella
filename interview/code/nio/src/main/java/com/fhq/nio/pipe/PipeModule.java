package com.fhq.nio.pipe;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class PipeModule extends AbstractModule {

   @Override
   protected void configure() {
      install(ThrowingProviderBinder.forModule(this));
      bind(new TypeLiteral<Callable<Long>>() {}).annotatedWith(Names.named("source")).to(PipeSourceThread.class)
            .in(Scopes.SINGLETON);
      bind(new TypeLiteral<Callable<Long>>() {}).annotatedWith(Names.named("sink")).to(PipeSinkThread.class)
            .in(Scopes.SINGLETON);
      bind(ExecutorService.class).annotatedWith(Names.named("pipe")).toInstance(Executors.newFixedThreadPool(5));
      bind(PipeDemo.class).in(Scopes.SINGLETON);
   }

   @CheckedProvides(PipeProvider.class)
   @Singleton
   Pipe providePipe() throws IOException {
      return Pipe.open();
   }

}
