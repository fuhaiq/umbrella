package com.fhq.algorithm.linear;

import com.fhq.algorithm.linear.linklist.Node;
import com.fhq.algorithm.linear.linklist.Node.Builder;
import com.fhq.algorithm.linear.linklist.NodePractice;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {

   public static void main(String[] args) throws Exception {
      Injector injector = Guice.createInjector(new LinearModule());
      
      Builder<Character> builder = Node.builder();
      var head = builder.clear().add('a', 'b', 'c', 'd', 'e').build();
      var practice = injector.getInstance(NodePractice.class);
      
      System.out.println("--------中间节点");
      practice.findMid(head).stream().forEach(System.out::println);
      
      System.out.println("--------逆转链表");
      head = practice.reverse(head);
      head.stream().forEach(System.out::println);
      
      System.out.println("--------给链表做环");
      head = practice.cycle(head, 2);
      
      System.out.println("--------链表环检测");
      var op = practice.hasCycle(head);
      if(op.isPresent()) {
         System.out.println("检测到环");
         System.out.println("环进入点为:"+op.get().toString());
      } else {
         System.out.println("没有环");
      }
      
      System.out.println("--------链表交叉");
      var first = builder.clear().add('a', 'b', 'c', 'd', 'e', 'f', 'g').build();
      var second = builder.clear().add('1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '*').build();
      
      var intersect = practice.intersect(first, second, 3, 7);
      first = intersect.get(0);
      second = intersect.get(1);
      System.out.println("--------交叉完成");
      System.out.println("--------交叉检查");
      op = practice.hasIntersect(first, second);
      if(op.isPresent()) {
         System.out.println("检测到交叉");
         System.out.println("交叉点为:"+op.get().toString());
      } else {
         System.out.println("没有交叉");
      }
      
      System.out.println("--------子链表");
      head = builder.clear().add('a', 'b', 'c', 'd', 'e', 'f', 'g').build();
      practice.subList(head, 2, 5).stream().forEach(System.out::println);
      
      System.out.println("--------删除链表倒数第N个节点");
      head = builder.clear().add('a', 'b', 'c', 'd', 'e', 'f', 'g').build();
      practice.deleteFromTail(head, 4).stream().forEach(System.out::println);
      
      System.out.println("--------任意位置逆转链表");
      head = builder.clear().add('a', 'b', 'c', 'd', 'e', 'f', 'g').build();
      practice.reverse(head, 1, 5).stream().forEach(System.out::println);
      
      System.out.println("--------右移链表");
      for (var i = 0; i <= 10; i++) {
         head = builder.clear().add('W', '*', '*', '*', '*', '*', '*').build();
         practice.rightShift(head, i).stream().forEach(System.out::print);
         System.out.println();
      }
      
      System.out.println("--------左移链表");
      for (var i = 0; i <= 10; i++) {
         head = builder.clear().add('W', '*', '*', '*', '*', '*', '*').build();
         practice.leftShift(head, i).stream().forEach(System.out::print);
         System.out.println();
      }
      
      System.out.println("--------划分链表");
      Builder<Integer> intBuilder = Node.builder();
      var node = intBuilder.clear().add(3,5,8,1,435,77,234,12,3,4).build();
      practice.divide(node, 10).stream().forEach(ele -> System.out.print(ele + "|"));
      
      
      System.out.println();
      System.out.println("--------链表相加");
      var l1 = intBuilder.clear().add(1,9).build();
      var l2 = intBuilder.clear().add(8,9,8).build();
      practice.add(l1, l2).stream().forEach(ele -> System.out.print(ele + "|"));
   }

}
