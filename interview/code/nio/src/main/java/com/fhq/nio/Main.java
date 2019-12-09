package com.fhq.nio;

import com.fhq.nio.pipe.select.PipeSelectDemo;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {

   public static void main(String[] args) throws Exception {
      Injector injector = Guice.createInjector(new NioModule());
      PipeSelectDemo demo = injector.getInstance(PipeSelectDemo.class);
      demo.show();
   }

}
