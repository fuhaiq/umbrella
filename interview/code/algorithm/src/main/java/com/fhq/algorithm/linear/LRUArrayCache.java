package com.fhq.algorithm.linear;

import java.util.logging.Logger;
import com.google.inject.Inject;
import static com.google.common.base.Preconditions.checkNotNull;

public class LRUArrayCache implements LRUCache {

   @Inject
   private Logger logger;

   private int capacity = 5;

   private int size = 0;

   private Object[] list = new Object[capacity];

   @Override
   public Object get(Object obj) {
      checkNotNull(obj, "参数不能为空");
      for (var current = 0; current < size; current++) {
         if (obj.equals(list[current])) {
            logger.info(String.format("命中缓存，直接返回缓存数据%s", obj.toString()));
            // 找到
            var temp = list[current];
            // 从当前位减一，向后移动
            for (var i = current - 1; i >= 0; i--) {
               list[i + 1] = list[i];
            }
            list[0] = temp;
            return list[0];
         }
      }
      // 没找到
      if (size < capacity) {
         logger.info(String.format("缓存未满，缓存数据%s", obj.toString()));
         for (var i = size - 1; i >= 0; i--) {
            list[i + 1] = list[i];
         }
         list[0] = obj;
         size++;
         return list[0];
      } else {
         logger.info(
               String.format("缓存已满，清除最后一个数据%s，并缓存数据%s", list[size - 1].toString(), obj.toString()));
         for (var i = size - 2; i >= 0; i--) {
            list[i + 1] = list[i];
         }
         list[0] = obj;
         return list[0];
      }
   }
}
