package com.fhq.nio;

import com.fhq.nio.channel.NioChannelModule;
import com.fhq.nio.conf.NioConfModule;
import com.fhq.nio.pipe.PipeModule;
import com.fhq.nio.pipe.select.PipeSelectModule;
import com.fhq.nio.socket.SocketModule;
import com.google.inject.AbstractModule;

public class NioModule extends AbstractModule {

   @Override
   protected void configure() {
      install(new NioConfModule());
      install(new NioChannelModule());
      install(new SocketModule());
      install(new PipeModule());
      install(new PipeSelectModule());
   }

}
