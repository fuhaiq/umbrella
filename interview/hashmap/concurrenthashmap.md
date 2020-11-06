# 数组初始化、添加元素

sizeCtl = 初始化数组长度= `initialCapacity + (initialCapacity >>> 1) + 1` 也就是说传入32,实际为64

```java
public ConcurrentHashMap(int initialCapacity) {
  if (initialCapacity < 0)
      throw new IllegalArgumentException();
  int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
             MAXIMUM_CAPACITY :
             tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
  this.sizeCtl = cap;
}
```

计算元素的hash值,将符号位置0,所以得到的hash值一定是正数
```java
static final int HASH_BITS = 0x7fffffff;
static final int spread(int h) {
  return (h ^ (h >>> 16)) & HASH_BITS;
}
```

初始化数组,原子操作sizeCtl=-1,表示正在初始化,完成后sizeCtl=扩容阈值
```java
private final Node<K,V>[] initTable() {
  Node<K,V>[] tab; int sc;
  while ((tab = table) == null || tab.length == 0) { //自旋
      if ((sc = sizeCtl) < 0) //如果<0,说明有别的线程正在初始化,让出
          Thread.yield(); // lost initialization race; just spin
      else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) { //原子操作,只有一个线程进入
          try {
              if ((tab = table) == null || tab.length == 0) {
                  int n = (sc > 0) ? sc : DEFAULT_CAPACITY; //数组长度,默认16
                  @SuppressWarnings("unchecked")
                  Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                  table = tab = nt;
                  sc = n - (n >>> 2); //0.75
              }
          } finally {
              sizeCtl = sc;//初始化完成后,将sizeCtl=扩容阈值
          }
          break;
      }
  }
  return tab;
}
```

lazy-init方式初始化数组
添加元素时
  1. 当前的 table[ (n - 1) & hash ] == null 时，采用CAS操作
  2. 当产生hash冲突时，采用synchronized锁头节点

注意**双重检查**,当前节点是否和之前锁住的节点是同一个.

因为在扩容数据迁移逻辑，也是对当前bucket头节点进行`synchronized`。会导致如下现象:

  1. 迁移线程获得锁，进入数据迁移，Put线程等待，迁移完成，插入FWD头节点，释放锁。Put线程获得锁，此时head已经发生变化。所以需要双重检查。- 检查失败，自旋再次获取，发现head变成了FWD，帮助扩容
  2. Put线程获得锁，进入数据插入，迁移线程等待，**尾部** 插入完成，释放锁。迁移线程获得锁，此时head无变化，进行迁移即可

```java
synchronized (f) {
  if (tabAt(tab, i) == f) { // 双重检查
```

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
  if (key == null || value == null) throw new NullPointerException();
  int hash = spread(key.hashCode());
  int binCount = 0;
  for (Node<K,V>[] tab = table;;) {//自旋
      Node<K,V> f; int n, i, fh;
      if (tab == null || (n = tab.length) == 0)
          tab = initTable();//初始化
      else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {//Bucket为空,即第一次添加元素
          if (casTabAt(tab, i, null,
                       new Node<K,V>(hash, key, value, null)))//原子比较并赋值,只有一个线程能进来
              break;                   // no lock when adding to empty bin
      }
      else if ((fh = f.hash) == MOVED)//forward节点的hash值,表示正在扩容
          tab = helpTransfer(tab, f);
      else { // hash 冲突,采用synchronized
          V oldVal = null;
          synchronized (f) { //牛逼!! 只锁bucket头节点,不影响其他bucket
              if (tabAt(tab, i) == f) { // 防止其他线程把当前bucket链表转树,或扩容. 位置会发生变化
                  if (fh >= 0) { // 链表结构
                      binCount = 1;
                      for (Node<K,V> e = f;; ++binCount) { // 遍历链表
                          K ek;
                          if (e.hash == hash &&
                              ((ek = e.key) == key ||
                               (ek != null && key.equals(ek)))) { //替换
                              oldVal = e.val;
                              if (!onlyIfAbsent)
                                  e.val = value;
                              break;
                          }
                          Node<K,V> pred = e;
                          if ((e = e.next) == null) {
                              pred.next = new Node<K,V>(hash, key,
                                                        value, null); //尾结点添加
                              break;
                          }
                      }
                  }
                  else if (f instanceof TreeBin) { //red-black tree
                      Node<K,V> p;
                      binCount = 2;
                      if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                     value)) != null) {
                          oldVal = p.val;
                          if (!onlyIfAbsent)
                              p.val = value;
                      }
                  }
              }
          } // 释放锁
          if (binCount != 0) {
              if (binCount >= TREEIFY_THRESHOLD) // >=8
                  treeifyBin(tab, i); // 链表转树,但是如果数组长度 < 64,扩容. 否则转树
              if (oldVal != null)
                  return oldVal;
              break;
          }
      }
  }
  addCount(1L, binCount); //更新元素个数,并判断是否要扩容. binCount为当前bucket里面的元素个数
  return null;
}
```

并不是达到8就树化链表
```java
private final void treeifyBin(Node<K,V>[] tab, int index) {
  Node<K,V> b; int n, sc;
  if (tab != null) {
      if ((n = tab.length) < MIN_TREEIFY_CAPACITY) // 64
          tryPresize(n << 1);
      else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
          synchronized (b) {
              if (tabAt(tab, index) == b) {
                  ...
              }
          }
      }
  }
}
```

# 添加元素后长度+1

此操作同时负责扩容

`baseCount` 为内部一个基础长度,首先会原子`baseCount + 1`,如果不成功往`CounterCell[]`这个数组里面对应位置的value+1,如果加不上,自旋回来继续加,直到加上.最终的元素长度=`baseCount + sum(CounterCell[])`

```java
private final void addCount(long x, int check) { // x = 1L, 当操作为put时,check为binCount,肯定>=1
  CounterCell[] as; long b, s;
  if ((as = counterCells) != null ||
      !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) { //原子baseCount + 1,加成功了就进不来了,意思是baseCount+1成功了,不用再加CounterCell了
      CounterCell a; long v; int m;
      boolean uncontended = true;
      if (as == null || (m = as.length - 1) < 0 ||
          (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
          !(uncontended =
            U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
          fullAddCount(x, uncontended); //第一次加数组
          return;
      }
      if (check <= 1)
          return;
      s = sumCount();
  }
  if (check >= 0) { //当put操作时肯定能进来. 移除、替换元素时不会进来
      Node<K,V>[] tab, nt; int n, sc;
      while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
             (n = tab.length) < MAXIMUM_CAPACITY) { // 此时sizeCtl=扩容阈值
          int rs = resizeStamp(n); // n 肯定大于0
          if (sc < 0) { // sizeCtl < 0 表示正在扩容,进行协助扩容
              if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                  sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                  transferIndex <= 0)
                  break;
              if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) // sizeCtl = 当前扩容线程数目 + 1
                  transfer(tab, nt); //协助扩容
          }
          else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                       (rs << RESIZE_STAMP_SHIFT) + 2)) // 第一次进来时sc=扩容阈值,肯定>0,所以进入此逻辑
                                       // rs向左移动16位,因为rs第17位=1,所以左移16后原第17位变成最高位32位,导致结果为负数
                                       // 将sizeCtl赋值<0,表示当前正在扩容
                                       // 下一个线程进来后发现sc<0,走上面协助扩容逻辑
              transfer(tab, null); //扩容
          s = sumCount();
      }
  }
}
```

```java
static final int resizeStamp(int n) {
  //1向左移动16位后或运算。即将第17位置1
  return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
}
```

### Integer.numberOfLeadingZeros

该方法的作用是返回无符号整型i的最高非零位前面的0的个数，包括符号位在内。如果i为负数，将会返回0

### Integer.numberOfTrailingZeros
该方法的作用是返回无符号整型i的最低非零位后面的0的个数。如果最低位为1，numberOfTrailingZeros返回0

```java
Integer.numberOfLeadingZeros(65536); // 15
Integer.numberOfTrailingZeros(65536); // 16
Integer.numberOfTrailingZeros(3); // 0
```

# 扩容

将原有数组划分成N块，每个线程负责其中一块的数据迁移工作。`stride`表示每块的数据大小。从后向前数据迁移

当前bucket迁移完成后或当前bucket为null，在bucket的头节点插入一个`ForwardingNode`节点，表示当前bucket已经迁移完成，但数组正在进行扩容中. 所以别的线程调用`putVal`时发现 `head.hash == MOVED` 就会过来帮助一起扩容

```java

static final class ForwardingNode<K,V> extends Node<K,V> {
  final Node<K,V>[] nextTable;
  ForwardingNode(Node<K,V>[] tab) {
      super(MOVED, null, null, null); // hash code = MOVED = -1
      this.nextTable = tab;
  }...
}


/**
 * The next table to use; non-null only while resizing.
 */
private transient volatile Node<K,V>[] nextTable;


private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
  int n = tab.length, stride;
  //每个线程负责一部分的数据迁移工作
  /*
  计算方法为：CPU核数 > 1?
    是：(数组长度 >>> 3) / CPu核数
    否：数组长度
  如果结果 < 16,那么 = 16
  */
  if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
      stride = MIN_TRANSFER_STRIDE; // subdivide range
  if (nextTab == null) {            // initiating
      try {
          @SuppressWarnings("unchecked")
          Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1]; // 容量*2
          nextTab = nt;
      } catch (Throwable ex) {      // try to cope with OOME
          sizeCtl = Integer.MAX_VALUE;
          return;
      }
      nextTable = nextTab;
      transferIndex = n; // 旧数组的size
  }
  int nextn = nextTab.length;
  ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
  boolean advance = true;
  boolean finishing = false; // to ensure sweep before committing nextTab
  for (int i = 0, bound = 0;;) {
      Node<K,V> f; int fh;
      while (advance) {
          int nextIndex, nextBound;
          if (--i >= bound || finishing)
              advance = false;
          else if ((nextIndex = transferIndex) <= 0) { // 将nextIndex首先赋值为当前数组长度
              i = -1;
              advance = false;
          }
          /*
          计算线程的任务边界，原子修改transferIndex变量 = nextIndex - 任务个数。
          即从后向前计算出当前线程的数据迁移边界
          如果nextIndex < stride 表示剩下待迁移的bucket数量小于单次任务数量，即最后一个迁移任务了。所以将nextIndex = 0
          */
          else if (U.compareAndSwapInt
                   (this, TRANSFERINDEX, nextIndex,
                    nextBound = (nextIndex > stride ?
                                 nextIndex - stride : 0))) {
              bound = nextBound;
              i = nextIndex - 1;
              advance = false;
          }
      }
      if (i < 0 || i >= n || i + n >= nextn) {
          int sc;
          if (finishing) { // 扩容全部完成
              nextTable = null;
              table = nextTab;
              sizeCtl = (n << 1) - (n >>> 1);
              return;
          }
          if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
              if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT) // 和 addCount 对应，判断是否扩容全部完成
                  return;
              finishing = advance = true;
              i = n; // recheck before commit
          }
      }
      else if ((f = tabAt(tab, i)) == null)
          advance = casTabAt(tab, i, null, fwd);
      else if ((fh = f.hash) == MOVED)
          advance = true; // already processed
      else {
          synchronized (f) { // 锁bucket头节点,和putVal是一个锁
              if (tabAt(tab, i) == f) {
                  Node<K,V> ln, hn;
                  if (fh >= 0) {
                      int runBit = fh & n;
                      Node<K,V> lastRun = f;
                      for (Node<K,V> p = f.next; p != null; p = p.next) {
                          int b = p.hash & n;
                          if (b != runBit) {
                              runBit = b;
                              lastRun = p;
                          }
                      }
                      if (runBit == 0) {
                          ln = lastRun;
                          hn = null;
                      }
                      else {
                          hn = lastRun;
                          ln = null;
                      }
                      for (Node<K,V> p = f; p != lastRun; p = p.next) {
                          int ph = p.hash; K pk = p.key; V pv = p.val;
                          if ((ph & n) == 0)
                              ln = new Node<K,V>(ph, pk, pv, ln);
                          else
                              hn = new Node<K,V>(ph, pk, pv, hn);
                      }
                      setTabAt(nextTab, i, ln);
                      setTabAt(nextTab, i + n, hn);
                      setTabAt(tab, i, fwd); // 迁移完成后,插入FWD节点
                      advance = true;
                  }
                  else if (f instanceof TreeBin) {
                      TreeBin<K,V> t = (TreeBin<K,V>)f;
                      TreeNode<K,V> lo = null, loTail = null;
                      TreeNode<K,V> hi = null, hiTail = null;
                      int lc = 0, hc = 0;
                      for (Node<K,V> e = t.first; e != null; e = e.next) {
                          int h = e.hash;
                          TreeNode<K,V> p = new TreeNode<K,V>
                              (h, e.key, e.val, null, null);
                          if ((h & n) == 0) {
                              if ((p.prev = loTail) == null)
                                  lo = p;
                              else
                                  loTail.next = p;
                              loTail = p;
                              ++lc;
                          }
                          else {
                              if ((p.prev = hiTail) == null)
                                  hi = p;
                              else
                                  hiTail.next = p;
                              hiTail = p;
                              ++hc;
                          }
                      }
                      ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                          (hc != 0) ? new TreeBin<K,V>(lo) : t;
                      hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                          (lc != 0) ? new TreeBin<K,V>(hi) : t;
                      setTabAt(nextTab, i, ln);
                      setTabAt(nextTab, i + n, hn);
                      setTabAt(tab, i, fwd);
                      advance = true;
                  }
              }
          }
      }
  }
}
```


# 查找元素

```java
public V get(Object key) {
  Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
  int h = spread(key.hashCode());
  if ((tab = table) != null && (n = tab.length) > 0 &&
      (e = tabAt(tab, (n - 1) & h)) != null) { // 当前数组不为空,即已经初始化完成
      if ((eh = e.hash) == h) { // 如果匹配则返回数据
          if ((ek = e.key) == key || (ek != null && key.equals(ek)))
              return e.val;
      }
      else if (eh < 0) // 如果头节点hash<0,找到的是一个FWD节点。即正在扩容,调用ForwardingNode.find
          return (p = e.find(h, key)) != null ? p.val : null;
      while ((e = e.next) != null) { // 进行链表、树遍历查询
          if (e.hash == h &&
              ((ek = e.key) == key || (ek != null && key.equals(ek))))
              return e.val;
      }
  }
  return null;
}


static final class ForwardingNode<K,V> extends Node<K,V> {
  final Node<K,V>[] nextTable;
  ForwardingNode(Node<K,V>[] tab) {
      super(MOVED, null, null, null);
      this.nextTable = tab; //新数组
  }

  Node<K,V> find(int h, Object k) {
      // loop to avoid arbitrarily deep recursion on forwarding nodes
      outer: for (Node<K,V>[] tab = nextTable;;) {
          Node<K,V> e; int n;
          if (k == null || tab == null || (n = tab.length) == 0 ||
              (e = tabAt(tab, (n - 1) & h)) == null)
              return null;
          for (;;) {
              int eh; K ek;
              if ((eh = e.hash) == h &&
                  ((ek = e.key) == k || (ek != null && k.equals(ek))))
                  return e;
              if (eh < 0) { // 如果是一个FWD节点
                  if (e instanceof ForwardingNode) {
                      tab = ((ForwardingNode<K,V>)e).nextTable;
                      continue outer;
                  }
                  else
                      return e.find(h, k);
              }
              if ((e = e.next) == null)
                  return null;
          }
      }
  }
}
```
