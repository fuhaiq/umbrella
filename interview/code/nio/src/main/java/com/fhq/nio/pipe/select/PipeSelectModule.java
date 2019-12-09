package com.fhq.nio.pipe.select;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.util.List;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.throwingproviders.CheckedProvides;
import com.google.inject.throwingproviders.ThrowingProviderBinder;

public class PipeSelectModule extends AbstractModule {

   @Override
   protected void configure() {
      install(ThrowingProviderBinder.forModule(this));
      install(new FactoryModuleBuilder().implement(Runnable.class, PipeSinkThread.class)
            .build(RunnableFactory.class));
      bind(MsgHandler.class).in(Scopes.SINGLETON);
      bind(PipeSelectDemo.class).in(Scopes.SINGLETON);
   }

   @CheckedProvides(PipesProvider.class)
   @Singleton
   List<Pipe> providePipes() throws IOException {
      return List.of(Pipe.open(), Pipe.open(), Pipe.open());
   }

}
