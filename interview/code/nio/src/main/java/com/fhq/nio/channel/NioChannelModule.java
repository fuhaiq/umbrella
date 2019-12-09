package com.fhq.nio.channel;

import com.google.common.io.ByteSource;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class NioChannelModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(ByteSource.class).toProvider(ByteSourceProvider.class).in(Scopes.SINGLETON);
      bind(SimpleBufferDemo.class).in(Scopes.SINGLETON);
   }

}
