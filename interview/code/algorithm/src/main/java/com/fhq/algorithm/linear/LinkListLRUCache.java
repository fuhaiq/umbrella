package com.fhq.algorithm.linear;

import java.util.LinkedList;
import java.util.logging.Logger;
import com.google.inject.Inject;
import static com.google.common.base.Preconditions.checkNotNull;

public class LinkListLRUCache implements LRUCache {
   
   @Inject
   private Logger logger;

   private LinkedList<Object> list = new LinkedList<Object>();

   private int capacity = 5;
   
   @Override
   public Object get(Object t) {
      checkNotNull(t, "参数不能为空");
      if(list.contains(t)) {
         logger.info(String.format("命中缓存，直接返回缓存数据%s", t.toString()));
         list.remove(t);
         list.push(t);
         return t;
      }
      var size = list.size();
      if(size < capacity) {
         logger.info(String.format("缓存未满，缓存数据%s", t.toString()));
         list.push(t);
      }
      if(size >= capacity) {
         var last = list.removeLast();
         logger.info(String.format("缓存已满，清除最后一个数据%s，并缓存数据%s", last.toString(), t.toString()));
         list.push(t);
      }
      return t;
   }

}
