package com.fhq.algorithm.linear.linklist;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkState;
import java.util.List;
import java.util.Optional;

public class NodePractice {

   public <T> Node<T> findMid(Node<T> head) {
      var slow = head;
      var fast = head;
      while (fast != null && fast.getNext() != null) {
         slow = slow.getNext();
         fast = fast.getNext().getNext();
      }
      return slow;
   }

   public <T> Node<T> reverse(Node<T> head) {
      Node<T> pre = null;
      Node<T> next = null;
      while (head != null) {
         next = head.getNext();
         head.setNext(pre);
         pre = head;
         head = next;
      }
      return pre;
   }

   public <T> Node<T> reverse(Node<T> head, int from, int to) {
      long size = head.stream().count();
      checkElementIndex(from, (int) size);
      checkElementIndex(to, (int) size);
      checkElementIndex(from, to);
      var guard = new Node<T>(null);
      guard.setNext(head);

      var left = guard.getNext();
      var right = guard.getNext();
      var leftConnectPoint = guard;

      var diff = to - from;
      while (diff-- > 0)
         right = right.getNext();
      while (from-- > 0) {
         leftConnectPoint = left;
         left = left.getNext();
         right = right.getNext();
      }
      var rightConnectPoint = right.getNext();
      right.setNext(null);

      left = reverse(left);
      leftConnectPoint.setNext(left);

      while (left.getNext() != null) {
         left = left.getNext();
      }
      left.setNext(rightConnectPoint);
      return guard.getNext();
   }


   public <T> Node<T> cycle(Node<T> head, int entryPoint) {
      long size = head.stream().count();
      checkElementIndex(entryPoint, (int) size);
      var tail = head;
      var entry = head;
      for (var i = 0; i < size; i++) {
         if (i < entryPoint)
            entry = entry.getNext();
         if (tail.getNext() != null)
            tail = tail.getNext();
      }
      tail.setNext(entry);
      return head;
   }

   public <T> Optional<Node<T>> hasCycle(Node<T> head) {
      var slow = head;
      var fast = head;
      Node<T> meetPoint = null;
      while (fast != null && fast.getNext() != null) {
         slow = slow.getNext();
         fast = fast.getNext().getNext();
         if (slow == fast) {
            meetPoint = slow;
            break;
         }
      }

      if (meetPoint == null)
         return Optional.empty();
      slow = head;
      while (slow != meetPoint) {
         slow = slow.getNext();
         meetPoint = meetPoint.getNext();
      }
      return Optional.of(slow);
   }

   public <T> List<Node<T>> intersect(Node<T> first, Node<T> second, int firstIntersectPoint,
         int secondIntersectPoint) {
      long sizeFirst = first.stream().count();
      checkElementIndex(firstIntersectPoint, (int) sizeFirst);

      long sizeSecond = second.stream().count();
      checkElementIndex(secondIntersectPoint, (int) sizeSecond);

      var current = first;
      while (firstIntersectPoint-- > 0)
         current = current.getNext();

      var intersectPoint = second;
      for (var i = 0; i < sizeSecond; i++) {
         if (i < secondIntersectPoint) {
            intersectPoint = intersectPoint.getNext();
         } else {
            current.setNext(intersectPoint);
            break;
         }
      }
      return List.of(first, second);
   }

   public <T> Optional<Node<T>> hasIntersect(Node<T> first, Node<T> second) {
      long sizeFirst = first.stream().count();
      long sizeSecond = second.stream().count();
      Node<T> shortList = null;
      Node<T> longList = null;
      long diff = Math.abs(sizeFirst - sizeSecond);
      if (sizeFirst >= sizeSecond) {
         shortList = second;
         longList = first;
      } else {
         shortList = first;
         longList = second;
      }
      while (diff-- > 0)
         longList = longList.getNext();
      checkState(shortList.stream().count() == longList.stream().count(), "the size is different");
      while (shortList != null && longList != null) {
         if (shortList == longList)
            return Optional.of(shortList);
         shortList = shortList.getNext();
         longList = longList.getNext();
      }
      return Optional.empty();

   }

   public <T> Node<T> subList(Node<T> head, int from, int to) {
      long size = head.stream().count();
      checkElementIndex(from, (int) size);
      checkElementIndex(to, (int) size);
      checkElementIndex(from, to);
      var left = head;
      var right = head;
      var diff = to - from;
      while (diff-- > 0)
         right = right.getNext();
      while (from-- > 0) {
         left = left.getNext();
         right = right.getNext();
      }
      right.setNext(null);
      return left;
   }

   public <T> Node<T> deleteFromTail(Node<T> head, int index) {
      checkState(index >= 0, "index must >= 0");
      long size = head.stream().count();
      checkElementIndex(index, (int) size);
      var guard = new Node<T>(null);
      guard.setNext(head);
      var left = guard.getNext();
      var right = guard.getNext();
      var breakPoint = guard;
      while (index-- >= 0)
         right = right.getNext();
      while (right != null) {
         breakPoint = left;
         left = left.getNext();
         right = right.getNext();
      }
      breakPoint.setNext(left.getNext());
      return guard.getNext();
   }

   public <T> Node<T> rightShift(Node<T> head, int step) {
      checkState(step >= 0, "index must >= 0");
      long size = head.stream().count();
      step = step % ((int) size);
      if (step == 0)
         return head;
      var guard = new Node<T>(null);
      guard.setNext(head);


      var left = guard.getNext();
      var right = guard.getNext();
      var breakPoint = guard;

      while (step-- > 1)
         right = right.getNext();
      while (right.getNext() != null) {
         breakPoint = left;
         left = left.getNext();
         right = right.getNext();
      }

      breakPoint.setNext(null);
      right.setNext(guard.getNext());
      return left;
   }

   public <T> Node<T> leftShift(Node<T> head, int step) {
      checkState(step >= 0, "index must >= 0");
      long size = head.stream().count();
      step = step % ((int) size);
      if (step == 0)
         return head;

      var breakPoint = head;
      var tail = head;
      while (step-- > 1) {
         breakPoint = breakPoint.getNext();
         tail = tail.getNext();
      }
      while (tail.getNext() != null)
         tail = tail.getNext();
      tail.setNext(head);
      head = breakPoint.getNext();
      breakPoint.setNext(null);
      return head;
   }
   
   public <T extends Comparable<T>> Node<T> divide(Node<T> head, T factor) {
      var leftGuard = new Node<T>(null);
      var leftTail = leftGuard;
      
      var rightGuard = new Node<T>(null);
      var rightTail = rightGuard;
      while(head != null) {
         var data = head.getData();
         if(data.compareTo(factor) < 0) {
            var next = new Node<T>(data);
            leftTail.setNext(next);
            leftTail = next;
         } else {
            var next = new Node<T>(data);
            rightTail.setNext(next);
            rightTail = next;
         }
         head = head.getNext();
      }
      leftTail.setNext(rightGuard.getNext());
      return leftGuard.getNext();
   }
   
   public Node<Integer> add(Node<Integer> first, Node<Integer> second) {
      first = reverse(first);
      second = reverse(second);
      var guard = new Node<Integer>(null);
      var current = guard;
      var carry = 0;
      while(first != null || second != null) {
         int x = (first == null) ? 0 : first.getData();
         int y = (second == null) ? 0 : second.getData();
         int sum = x + y + carry;
         carry = sum / 10;
         current.setNext(new Node<Integer>(sum % 10));
         current = current.getNext();
         if(first != null) first = first.getNext();
         if(second != null) second = second.getNext();
      }
      if(carry > 0) current.setNext(new Node<Integer>(carry));
      return guard.getNext();
   }

}
