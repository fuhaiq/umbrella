package com.fhq.algorithm.linear.linklist;

import java.util.stream.Stream;

public class Node<T> {
   private T data;
   private Node<T> next;

   public Node(T data) {
      this.data = data;
   }

   public Node(T data, Node<T> next) {
      this.data = data;
      this.next = next;
   }

   public T getData() {
      return data;
   }


   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((data == null) ? 0 : data.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Node other = (Node) obj;
      if (data == null) {
         if (other.data != null)
            return false;
      } else if (!data.equals(other.data))
         return false;
      return true;
   }

   public void setData(T data) {
      this.data = data;
   }

   public Node<T> getNext() {
      return next;
   }

   public void setNext(Node<T> next) {
      this.next = next;
   }

   @Override
   public String toString() {
      return "Node [data=" + data + "]";
   }

   public static <T> Builder<T> builder() {
      return new NodeBuilder<T>();
   }

   public Stream<T> stream() {
      java.util.stream.Stream.Builder<T> builder = Stream.builder();
      builder.add(getData());
      var current = getNext();
      while (current != null) {
         builder.add(current.getData());
         current = current.getNext();
      }
      return builder.build();
   }

   public interface Builder<T> {

      Builder<T> add(T... t);

      Node<T> build();
      
      Builder<T> clear();
   }
}
