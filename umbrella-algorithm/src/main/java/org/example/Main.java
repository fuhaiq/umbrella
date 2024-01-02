package org.example;

public class Main {
  public static void main(String[] args) {
    var ret = sum(100);
    System.out.println(ret);
  }

  public static int tribonacci(int n) {
    if (n == 0) return 0;
    if (n == 1) return 1;
    if (n == 2) return 1;
    return tribonacci(n - 3) + tribonacci(n - 2) + tribonacci(n - 1);
  }

  public static int sum(int n) {
    if (n <= 1) return n;
    return n + sum(n - 1);
  }
}
