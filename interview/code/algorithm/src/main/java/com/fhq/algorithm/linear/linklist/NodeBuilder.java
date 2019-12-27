package com.fhq.algorithm.linear.linklist;

import com.fhq.algorithm.linear.linklist.Node.Builder;

public class NodeBuilder<T> implements Node.Builder<T> {

   private Node<T> guard = new Node<T>(null);

   @Override
   public Node<T> build() {
      return guard.getNext();
   }

   @Override
   public Builder<T> add(T... t) {
      var tail = guard;
      while (tail.getNext() != null) {
         tail = tail.getNext();
      }
      for (T ele : t) {
         var next = new Node<T>(ele);
         tail.setNext(next);
         tail = next;
      }
      return this;
   }

   @Override
   public Builder<T> clear() {
      guard.setNext(null);
      return this;
   }


}
