package com.fhq.algorithm.linear;

import com.fhq.algorithm.linear.linklist.NodePractice;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class LinearModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(LRUCache.class).annotatedWith(Names.named("link")).to(LinkListLRUCache.class)
            .in(Scopes.SINGLETON);
      bind(LRUCache.class).annotatedWith(Names.named("array")).to(LRUArrayCache.class)
            .in(Scopes.SINGLETON);
      bind(NodePractice.class).toInstance(new NodePractice());
   }

}
