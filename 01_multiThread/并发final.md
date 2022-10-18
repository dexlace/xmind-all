

# 并发final

## 一、ThreadLocal

提供==线程局部变量==；一个线程局部变量在多个线程中，分别有==独立的值（副本）==

特点：**简单（开箱即用）、快速（无额外开销）、安全（线程安全）**

实现原理：java中使用哈希表实现

应用范围：几乎所有提供多线程特征的语言都提供

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220921154322628.png" alt="image-20220921154322628" style="zoom: 33%;" />

```java
// 构造函数 
ThreadLocal<T>()
// 初始化 initialValue()方法是延迟加载的，只有在调用get()方法的时候，才会被触发
// 当线程第一次使用get方法访问变量时，将调用此方法，除非线程先前调用了set方法，在这种情况下不会调用initialValue()
// 被remove之后重新调用get会被触发 每个线程最多执行一次该方法
// 不重写本方法  这个方法会返回null 一般使用匿名内部类的方法来重写initialValue()方法
initialValue()
// 访问器 
get/set
// 回收 
remove
```

```java
public class Basic {

    public static ThreadLocal<Long> x=new ThreadLocal<Long>(){
        @Override
        protected Long initialValue() {
            System.out.println("初始化方法将会执行一次");
            return 100L;
        }
    };

    public static void main(String[] args) {
        System.out.println(x.get());
        System.out.println(x.get());
    }
}

```

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220922100334913.png" alt="image-20220922100334913" style="zoom:50%;" />

**如果没有get，initialValue将不执行**

```java
public class Basic2 {

    public static ThreadLocal<Long> x=new ThreadLocal<Long>(){
        @Override
        protected Long initialValue() {
            System.out.println("初始化方法将会执行一次"+Thread.currentThread().getId());
            return Thread.currentThread().getId();
        }
    };

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("self thread get: "+x.get());
            }
        }).start();
        // x.set(109L); // 假设这一行执行 则main线程的初始化方法也不会跑了
        // x.remove(); // 假设这一行执行  main线程的threadLocal将会被清除  重新使用get方法的话会触发initialValue()方法
        // 运行结果 两个线程id不一样
        System.out.println("main get: "+x.get());
    }
}
```

### 1.1 使用场景

#### 1.1.1 资源持有

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220922101729762.png" alt="image-20220922101729762" style="zoom: 33%;" />

#### 1.1.2 线程资源一致性

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220922102010211.png" alt="image-20220922102010211" style="zoom:50%;" />

#### 1.1.3 线程安全

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220922102251735.png" alt="image-20220922102251735" style="zoom:33%;" />

#### 1.1.4 并发计算

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220922102353092.png" alt="image-20220922102353092" style="zoom:33%;" />

```java
@RestController
public class StatController {
    static  ThreadLocal<Integer> c= ThreadLocal.withInitial(() -> 0);

//    static Integer c;
    @RequestMapping("/stat")
    public Integer stat(){
//        return c;
        return c.get();
    }


    @RequestMapping("/add")
    public Integer add() throws InterruptedException {
        Thread.sleep(100);
//        c++; // 或者同步这个方法块
        c.set(c.get()+1);
        return 1;
    }

}

// 基于线程池模型synchronize（排队操作很危险）
// 用ThreadLocal收集数据很快且安全
// 思考：如何在多个ThreadLocal中收集数据
```

### 1.2 举例

**以下解决在多个ThreadLocal中收集数据**，是一个==并发计算的例子==

#### 1.2.1 并发计算

```java
@RestController
public class StatController {

    static HashSet<Val<Integer>> set=new HashSet<>();

    static  synchronized  void addSet(Val<Integer> v){
        set.add(v);
    }
    static  ThreadLocal<Val<Integer>> c= new ThreadLocal<Val<Integer> >(){
        @Override
        protected Val<Integer> initialValue() {
            // 所以每个线程第一次调用get将会触发一次
            // 所以这里set的size等于收集的线程数
            Val<Integer> val=new Val<>();
            // val只存在当前线程中 后续行为也是在该线程中  所以set没有问题
            val.set(0);
            // 临界区  会有同步问题  需要加锁
            // 由于每个线程只执行一次 所以效率问题不大
            //  set是个外部的变量 是个临界区
//            set.add(val);
            addSet(val);
            return val;
        }
    };


    @RequestMapping("/stat")
    public Integer stat(){
        // 汇总多个线程的结果
        return  set.stream().map(Val::get).reduce(Integer::sum).get();

    }


    @RequestMapping("/add")
    public Integer add() throws InterruptedException {
        Thread.sleep(100);
        // 拿到当前的线程的val
        // 所以每个线程第一次调用get将会触发一次初始化方法
        Val<Integer> v=c.get();
        v.set(v.get()+1);
        return 1;
    }

}

```

```bash
ab -n 10000 -c 100 localhost:8080/add  # 压测
curl localhost:8080/stat  
```

#### 1.2.2 线程安全

每个线程需要一个独享的对象，通常是工具类，典型的使用类有`SimpleDateFormat`和`Random`

假设一千个线程都在打印日期，==并用了1000个`SimpleDateFormat`，则造成了多余的内存消耗==

假设一千个线程都在打印日期，==并将`SimpleDateFormat`对象静态化，且打印过程不加锁，则有线程安全问题==

```java
/**
 * 描述：     1000个打印日期的任务，用线程池来执行
 * 所有线程公用一个SimpleDateFormat对象时出现错误
 * 有线程打印的是相同的日期
 * 出现线程安全问题
 */
public class ThreadLocalNormalUsage03 {

    public static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    String date = new ThreadLocalNormalUsage03().date(finalI);
                    System.out.println(date);
                }
            });
        }
        threadPool.shutdown();
    }

    public String date(int seconds) {
        //参数的单位是毫秒，从1970.1.1 00:00:00 GMT计时
        Date date = new Date(1000 * seconds);
        return dateFormat.format(date);
    }
}
```

而对打印日期的函数进行加锁，则是一种非常不佳的做法，效率低

使用threadLocal

```java
/**
 * 描述：     利用ThreadLocal，给每个线程分配自己的dateFormat对象，保证了线程安全，高效利用内存
 */
public class ThreadLocalNormalUsage05 {

    public static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>(){
        // 重写初始化
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    // lambda表达式
    public static ThreadLocal<SimpleDateFormat> dateFormatThreadLocal2 = ThreadLocal
            .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            int finalI = i;
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    String date = new ThreadLocalNormalUsage05().date(finalI);
                    System.out.println(date);
                }
            });
        }
        threadPool.shutdown();
    }

    public String date(int seconds) {
        //参数的单位是毫秒，从1970.1.1 00:00:00 GMT计时
        Date date = new Date(1000 * seconds);
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormat = dateFormatThreadLocal2.get();
        return dateFormat.format(date);
    }
}
```

#### 1.2.3 资源持有

每个线程内需要保存全局变量（例如在拦截器中获取用户信息），可以让不同方法直接使用，避免参数传递的麻烦

- 强调的是==同一个请求（同一个线程）内不同方法间的共享==

- ==不需要重写initialValue()方法，但必须手动调用set()方法==

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220923105728159.png" alt="image-20220923105728159" style="zoom: 50%;" />

```java
/**
 * 描述：     演示ThreadLocal用法2：避免传递参数的麻烦 保证当前线程传递参数的全局性
 */
public class ThreadLocalNormalUsage06 {

    public static void main(String[] args) {
        new Service1().process("");

    }
}

class Service1 {

    public void process(String name) {
        User user = new User("99");
        // 在这里设置值
        UserContextHolder.holder.set(user);
        new Service2().process();
    }
}

class Service2 {

    public void process() {
        User user = UserContextHolder.holder.get();
        System.out.println("Service2拿到用户名：" + user.name);
        new Service3().process();
    }
}

class Service3 {

    public void process() {
        User user = UserContextHolder.holder.get();
        System.out.println("Service3拿到用户名：" + user.name);
        UserContextHolder.holder.remove();
    }
}

class UserContextHolder {

    public static ThreadLocal<User> holder = new ThreadLocal<>();


}

class User {

    String name;

    public User(String name) {
        this.name = name;
    }
}
```

### 1.3 总结

#### 1.3.1 线程隔离

每个线程都有自己的独立对象

#### 1.3.2 线程内全局共享

在任何方法中都可以轻松获取到该对象

### 1.4 ThreadLocal注意点

#### 1.4.1 内存泄漏

## 二、锁

### 2.1 Lock接口

#### 2.1.1 为什么synchronized不够用

lock并不是来替代synchronized的，而是来提供高级功能的

**sychronized的不足**

- **效率低：**
- - ==锁的释放情况少==（<font color=red>异常情况可以释放</font>）
- - 试图获取锁时不能==设定超时==
- - ==不能中断==一个正在试图获得锁的线程（wait方法除外）
- **不够灵活**：加锁和解锁的时机单一
- **无法知道是否成功获取到锁**

#### 2.1.2 lock()

- 最普通的获取锁，如果被其他线程获取值，则进行等待
- lock==不会像synchronized一样在异常时自动释放锁==
- ==一定要在finally中释放锁，以保证异常发生时锁被释放==

- <font color=red>一旦陷入死锁，lock()就会陷入永久等待</font>

#### 2.1.3 tryLock()/tryLock(long time,TimeUnit unit)

- ==尝试获取锁==，如果当前锁没有被其他线程占用，则获取成功返回true，否则返回false
- 可以==根据是否成功决定后续行为==
- ==会立刻返回，即使拿不到不会一直等==

#### 2.1.4 lockInterruptibly()

#### 2.1.5 可见性保证

### 2.2 锁的分类

![image-20220920161903109](/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220920161903109-3661950.png)

#### 2.2.1 乐观锁和悲观锁

- **悲观锁（互斥同步锁）**：**==synchronized和Lock相关的类==**
- - ==阻塞和唤醒带来的性能劣势==
  - ==永久阻塞==：如果持有锁的线程被永久阻塞，那么等待该线程释放锁的那几个线程，将永远得不到执行
  - ==优先级反转==：如果等待的优先级较高，持有的优先级低，则造成优先级反转
  - ==数据库的`select for update`即悲观锁==
- **乐观锁：**==在修改数据时去检查是否被更改过==，<font color=red>**一般利用CAS算法实现**</font>
- - <font color=red>典型例子是原子类、并发容器等</font>
  - 应用场景比如==github的版本控制==，==给表加版本号==

- **开销对比**
- - 悲观锁原始开销高于乐观锁，特点是一劳永逸
  - 乐观锁一开始开销比较小，但如果自旋时间过长或者不停重试，消耗资源也会越来越多

- **使用场景对比**
- - 悲观锁：适合并发写入比较多的场景，可以避免大量的无用自旋
  - 乐观锁：适合并发写入少，大部分是读取的场景

#### 2.2.2 可重入锁和非可重入锁

- ReentrantLock、Sychronized都是可重入锁
- - 可重入性质依赖于AQS

```java
/**
 * 描述：     可重入性质
 */
public class RecursionDemo {

    private static ReentrantLock lock = new ReentrantLock();

    private static void accessResource() {
        lock.lock();
        try {
            System.out.println("已经对资源进行了处理");
            if (lock.getHoldCount()<5) {
                // 打印获取了几次
                System.out.println("获取锁次数："+lock.getHoldCount());
                accessResource();
                System.out.println("获取锁次数："+lock.getHoldCount());
            }
        } finally {
            lock.unlock();
        }
    }
    public static void main(String[] args) {
        accessResource();
    }
}

```

#### 2.2.3 公平锁和非公平锁

- **公平锁**：指的是按照线程请求的顺序，来分配锁；
- **非公平锁**：可以插队，但也是合适时机插队，提高效率

```java
private Lock queueLock = new ReentrantLock(true); // 公平
private Lock queueLock = new ReentrantLock(false); // 非公平
```

```java
public class FairLock {

    public static void main(String[] args) {
        Waiting waiting = new Waiting();
        Thread thread[] = new Thread[10];
        for (int i = 0; i < 10; i++) {
            thread[i] = new Thread(new DoSomething(waiting));
        }
        for (int i = 0; i < 10; i++) {
            thread[i].start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class DoSomething implements Runnable {

    Waiting waiting;

    public DoSomething(Waiting waiting) {
        this.waiting = waiting;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "开始");
        waiting.printJob(new Object());
        System.out.println(Thread.currentThread().getName() + "干活完毕");
    }
}

class Waiting {

    private Lock queueLock = new ReentrantLock(true);

    public void printJob(Object document) {
        queueLock.lock();
        try {
            int duration = new Random().nextInt(10) + 1;
            System.out.println(Thread.currentThread().getName() + "正在打第一份工，需要" + duration+"秒");
            Thread.sleep(duration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            queueLock.unlock();
        }

        // 打完第一份工 如果是公平的情况  则第二份工还会去排队
        // 如果是非公平的  打完第一份工  第二份工会继续
        queueLock.lock();
        try {
            int duration = new Random().nextInt(10) + 1;
            System.out.println(Thread.currentThread().getName() + "正在做兼职，需要" + duration+"秒");
            Thread.sleep(duration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            queueLock.unlock();
        }
    }
}
```

公平情况：

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220920180629695.png" alt="image-20220920180629695" style="zoom:50%;" />

非公平情况：

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220920181346106.png" alt="image-20220920181346106" style="zoom:50%;" />

**tryLock()方法不遵守设定的公平规则，一旦有线程释放了锁，那么这个正在tryLock的线程就能获得锁**，即使在他之前已经有其他线程在等待队列中。

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220920181901550.png" alt="image-20220920181901550" style="zoom:50%;" />

#### 2.2.4 共享锁和排他锁

- 排他锁：独占锁、独享锁
- 共享锁：又称读锁，获得共享锁之后可以查看但无法修改和删除，其他线程此时也可以获取到共享锁

集共享锁与排他锁性质的典型：**==ReentrantReadWriteLock==**

**读写锁的规则**

- 多个线程只申请读锁，都可以申请到
- 如果有一个线程已经**占用了读锁**，那么此时其他线程如果要申请写锁，则申请==写锁的线程会一直等待释放读锁==
- 如果有一个线程已经**占用了写锁**，那么其他线程如果申请写锁或者读锁，都无法申请

<font color=red>即要么多读，要么一写</font>

```java
public class CinemaReadWrite {

    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    private static void read() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到了读锁，正在读取");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放读锁");
            readLock.unlock();
        }
    }

    private static void write() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到了写锁，正在写入");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放写锁");
            writeLock.unlock();
        }
    }

    public static void main(String[] args) {
        new Thread(()->read(),"Thread1").start();
        new Thread(()->read(),"Thread2").start();
        new Thread(()->write(),"Thread3").start();
        new Thread(()->write(),"Thread4").start();
    }
}

```

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220921083912274.png" alt="image-20220921083912274" style="zoom:50%;" />

ReentrantReadWriteLock

- 公平锁的情况下：不允许插队
- 非公平锁的情况下：
- - **写锁可以随时插队**
  - **读锁**在**等待队列头节点**不是获取**写锁的线程**时可以插队
  - ==插队指的是申请锁的一瞬间的事，一旦进入等待队列后，就不能插队了==

```java
package com.dexlace.lock.readwrite;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 描述：     演示非公平和公平的ReentrantReadWriteLock的策略
 * 相关内容参考自defog tech
 */
public class NonfairBargeDemo {

   // 指定非公平情况
    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock(
            false);

    private static ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    private static void read() {
        System.out.println(Thread.currentThread().getName() + "开始尝试获取读锁");
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到读锁，正在读取");
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放读锁");
            readLock.unlock();
        }
    }

    private static void write() {
        System.out.println(Thread.currentThread().getName() + "开始尝试获取写锁");
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到写锁，正在写入");
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放写锁");
            writeLock.unlock();
        }
    }

    public static void main(String[] args) {
        new Thread(()->write(),"Thread1").start();
        new Thread(()->read(),"Thread2").start();
        new Thread(()->read(),"Thread3").start();
        new Thread(()->write(),"Thread4").start();
        new Thread(()->read(),"Thread5").start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread thread[] = new Thread[1000];
                for (int i = 0; i < 1000; i++) {
                    thread[i] = new Thread(() -> read(), "子线程创建的Thread" + i);
                }
                for (int i = 0; i < 1000; i++) {
                    thread[i].start();
                }
            }
        }).start();
    }
}

```

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220921094815003.png" alt="image-20220921094815003" style="zoom:50%;" />

Thread1释放写锁后，本该Thread2去读，但是Thread746获取到读锁了，因为此时等待队列的头节点是读线程

#### 2.2.5 锁的升降级（只支持降级）

```java
public class Upgrading {

    private static ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock(
            false);
    private static ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private static ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    private static void readUpgrading() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到了读锁，正在读取");
            Thread.sleep(1000);
            System.out.println("升级会带来阻塞");
            writeLock.lock();
            System.out.println(Thread.currentThread().getName() + "获取到了写锁，升级成功");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName() + "释放读锁");
            readLock.unlock();
        }
    }

    private static void writeDowngrading() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "得到了写锁，正在写入");
            Thread.sleep(1000);
            readLock.lock();
            System.out.println("在不释放写锁的情况下，直接获取读锁，成功降级");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            readLock.unlock();
            System.out.println(Thread.currentThread().getName() + "释放写锁");
            writeLock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("先演示降级是可以的");
        Thread thread1 = new Thread(() -> writeDowngrading(), "Thread1");
        thread1.start();
        thread1.join();
        System.out.println("------------------");
//        System.out.println("演示升级是不行的");
//        Thread thread2 = new Thread(() -> readUpgrading(), "Thread2");
//        thread2.start();
    }
}
```

为什么不能支持锁的升级

假设a,b都想升级，则a，b都必须放弃读锁，会造成死锁

#### 2.2.6 自旋锁和阻塞锁

- 阻塞或唤醒一个Java线程需要操作系统切换cpu状态来完成，这种状态转换需要耗费处理器时间
- 如果同步代码块中的内容过于简单，状态转换消耗的时间有可能比用户代码执行的时间还长
- 在许多场景中，同步资源的锁定时间很短，为了这一小段时间去切换线程，**线程挂起和恢复现场的花费可能会让系统得不偿失**
- 如果机器有多个处理器，能够让两个或以上的线程同时并行执行，我们可以让==**后面请求锁的线程不放弃cpu的执行时间**==，看看持有锁的线程是否很快就会释放锁
- 为了让当前线程“稍等一下”，我们需要让当前线程进行自旋，如果在自旋完成后前面锁定同步资源的线程已经释放了锁，那么当前线程就可以不必阻塞而是直接获取同步资源，从而避免切换线程的开销，即自旋锁。
- 即**“我稍等一会儿，先不走”**
- 阻塞锁相反

- ==如果锁被占用时间很长，那么自旋的线程会浪费处理器资源==
- 原子类都是用自旋锁实现的

手写一个自旋锁

```java
public class SpinLock {

    private AtomicReference<Thread> sign = new AtomicReference<>();

    public void lock() {
        Thread current = Thread.currentThread();
        while (!sign.compareAndSet(null, current)) {
            System.out.println(current.getName()+"自旋获取失败，再次尝试");
        }
    }

    public void unlock() {
        Thread current = Thread.currentThread();
        sign.compareAndSet(current, null);
    }

    public static void main(String[] args) {
        SpinLock spinLock = new SpinLock();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "开始尝试获取自旋锁");
                spinLock.lock();
                System.out.println(Thread.currentThread().getName() + "获取到了自旋锁");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    spinLock.unlock();
                    System.out.println(Thread.currentThread().getName() + "释放了自旋锁");
                }
            }
        };
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        thread1.start();
        thread2.start();
    }
}
```

#### 2.2.7 可中断锁和不可中断锁

- synchronized就不是可中断锁
- lock是可中断锁，因为tryLock(time)和lockInterruptibly()都能响应中断

### 2.3 锁优化

- java虚拟机对锁的优化
- - 自旋锁和自适应
  - 锁消除
  - 锁粗化

- 缩小同步代码块
- 尽量不要锁住方法
- 减少请求锁的次数
- 锁中不要再包含锁
- 选择合适的锁类型或合适的工具类

## 三、atomic包

### 3.1 原子类的作用

保证并发安全，粒度更细，保证在变量级别，效率更高，除了高度竞争情况

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20220921135250505.png" alt="image-20220921135250505" style="zoom: 33%;" />

`get()`

`getAndSet(int newValue)`

`getAndIncrement()`

`getAndAdd(int delta)`

`compareAndSet(int expect, int update)`

### 3.2 Atomic*Array数组型原子类

```java
public class AtomicArrayDemo {

    public static void main(String[] args) {
        AtomicIntegerArray atomicIntegerArray = new AtomicIntegerArray(1000);
        Incrementer incrementer = new Incrementer(atomicIntegerArray);
        Decrementer decrementer = new Decrementer(atomicIntegerArray);
        Thread[] threadsIncrementer = new Thread[100];
        Thread[] threadsDecrementer = new Thread[100];
        for (int i = 0; i < 100; i++) {
            threadsDecrementer[i] = new Thread(decrementer);
            threadsIncrementer[i] = new Thread(incrementer);
            threadsDecrementer[i].start();
            threadsIncrementer[i].start();
        }

//        Thread.sleep(10000);
        for (int i = 0; i < 100; i++) {
            try {
                threadsDecrementer[i].join();
                threadsIncrementer[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < atomicIntegerArray.length(); i++) {
//            if (atomicIntegerArray.get(i)!=0) {
//                System.out.println("发现了错误"+i);
//            }
            System.out.println(atomicIntegerArray.get(i));
        }
        System.out.println("运行结束");
    }
}

class Decrementer implements Runnable {

    private AtomicIntegerArray array;

    public Decrementer(AtomicIntegerArray array) {
        this.array = array;
    }

    @Override
    public void run() {
        for (int i = 0; i < array.length(); i++) {
            array.getAndDecrement(i);
        }
    }
}

class Incrementer implements Runnable {

    private AtomicIntegerArray array;

    public Incrementer(AtomicIntegerArray array) {
        this.array = array;
    }

    @Override
    public void run() {
        for (int i = 0; i < array.length(); i++) {
            array.getAndIncrement(i);
        }
    }
}

```

### 3.3 Atomic*Reference引用类型原子类

==让对象保持原子性==

```java
public class SpinLock {

    private AtomicReference<Thread> sign = new AtomicReference<>();

    public void lock() {
        Thread current = Thread.currentThread();
        while (!sign.compareAndSet(null, current)) {
            System.out.println(current.getName()+"自旋获取失败，再次尝试");
        }
    }

    public void unlock() {
        Thread current = Thread.currentThread();
        sign.compareAndSet(current, null);
    }

    public static void main(String[] args) {
        SpinLock spinLock = new SpinLock();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "开始尝试获取自旋锁");
                spinLock.lock();
                System.out.println(Thread.currentThread().getName() + "获取到了自旋锁");
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    spinLock.unlock();
                    System.out.println(Thread.currentThread().getName() + "释放了自旋锁");
                }
            }
        };
        Thread thread1 = new Thread(runnable);
        Thread thread2 = new Thread(runnable);
        thread1.start();
        thread2.start();
    }
}
```

### 3.4 AtomicIntegerFieldUpdate

==把普通变量升级为原子变量==

```java
public class AtomicIntegerFieldUpdaterDemo implements Runnable{

    static Student tom;
    static Student peter;

    // 提升Student中的myId
    public static AtomicIntegerFieldUpdater<Student> scoreUpdater = AtomicIntegerFieldUpdater
            .newUpdater(Student.class, "myId");

    @Override
    public void run() {
        for (int i = 0; i < 10000; i++) {
            peter.myId++;
            scoreUpdater.getAndIncrement(tom);
        }
    }

    public static class Student {

        volatile int myId;
    }

    public static void main(String[] args) throws InterruptedException {
        tom=new Student();
        peter=new Student();
        AtomicIntegerFieldUpdaterDemo r = new AtomicIntegerFieldUpdaterDemo();
        Thread t1 = new Thread(r);
        Thread t2 = new Thread(r);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println("普通变量："+peter.myId);
        System.out.println("升级后的结果"+ tom.myId);
    }
}

```

### 3.5 Adder累加器

比如LongAdder比AtomicLong效率高，本质是空间换时间

### 3.6 Accumalator累加器

## 四、CAS

略

==Unsafe类提供了硬件级别的原子操作==

## 五、final关键字和不变性

### 5.1 修饰属性

修饰变量，防止被修改

天生线程安全，不需要同步开销

- 如果是对象，那么==对象的引用不能变==，但是对象自身的内容可以变化

```java
final Person person=new Person();
// 再去new一个Person给person对象不支持
person=new Person();  // 不支持
// 但是person的变量中的值可以变
person.name="zhangsan";
```

#### 5.1.1 修饰属性

必须在以下三种时机赋值

- 声明时赋值
- ==构造函数中赋值==
- ==初始代码块中赋值==

#### 5.1.2 修饰静态属性

- 声明时赋值
- ==静态初始化代码块中赋值==

#### 5.1.3 修饰局部变量

- 不规定赋值时机，只要求在使用前必须赋值，这和方法中的非final变量的要求是一样的

### 5.2 修饰方法

- 构造方法不允许final
- 不可被重写

### 5.3 修饰类

修饰类则不可被继承

### 5.4 不变性与final的关系

- 不变性对于基本数据而言，确实被final修饰后具有不可变性

- 对于对象类型，需要保证该对象自身被创建后，状态永远不可变才可以
- - 对象创建后状态不可更改
  - 属性都是final修饰
  - 对象创建过程中没有发生逃逸

### 5.5 经典String面试题

```java
public static void main(String [] args){
  String a="hello2";
  
  final String b="hello";
  String d="hello";
  
  String c=b+2;
  String e=d+2;
  // true
  // 当final变量是基本数据类型以及String类型时，如果在编译期间能知道它的确切值，
  // 也就是提前知道了b的值，所以则编译器会把它当做编译期常量使用
  // 变量 c 是 b + 2得到的，由于 b 是一个常量，
  // 所以在使用 b 的时候直接相当于使用 b 的原始值（hello）进行计算，所以 c 生成的也是一个常量，
  // 而a 也是常量，a和c都是 hello2，由于 Java 中常量池中只生成唯一的一个hello2字符串，所以 a 和 c 是==的。
  System.out.println((a==c));
  // false
  // d是指向常量池中 hello，但由于 d 不是 final 修饰，
  // 也就是说编译器在使用 d 的时候不会提前知道 d 的值，所以在计算 e 的时候也需要在运行时才能确定，所以这种计算会在堆上生成 hello2,所以最终 e 指向的是堆上的，所以 a 和 e 不相等
  System.out.println((a==e));
}
```

```java
public class FinalStringDemo2 {

    public static void main(String[] args) {
        String a = "wukong2";
        // 编译期间b不会在编译时确定
        final String b = getDashixiong();
        String c = b + 2;
        // false
        System.out.println(a == c);

    }

    private static String getDashixiong() {
        return "wukong";
    }
}
```

## 六、并发容器

### 6.1 过时的Vector和Hashtable

并发安全但是性能不好

### 6.2 ArrayList和HashMap

虽然这两个类不是线程安全

但是可以用`Collections.synchronizedList(new ArrayList<E>())`和`Collections.synchronizedMap(new HashMap<K,V>())`使之变成线程安全

并没有比以上好太多

### 6.3 为什么HashMap是不安全的

- 同时`put`==碰撞==导致数据丢失
- 同时`put`==扩容==导致数据丢失
- ==死循环造成cpu100%（jdk1.7）==

#### 6.3.1 HashMap详解

##### 6.3.1.1 一些常量

```java
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {

    private static final long serialVersionUID = 362498820763181265L;
  
  
    // 默认初始容量
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    // 最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;

    // 默认的加载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

   
    // 
    static final int TREEIFY_THRESHOLD = 8;

    //
    static final int UNTREEIFY_THRESHOLD = 6;

    //
    static final int MIN_TREEIFY_CAPACITY = 64;

    

    // ......
}
```

##### 6.3.1.2 静态内部类之Node

```java
 static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        // 下一个node
        Node<K,V> next;

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

   			// 设置新的值 返回老的值
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        // 重写equals方法
        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }
```

##### 6.3.1.3 成员变量

```java
   /* ---------------- Fields -------------- */

    // 第一次使用时将会被初始化，在必要的时候将会扩容  长度是2的次幂
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    transient Set<Map.Entry<K,V>> entrySet;

    // size
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    // 扩容次数？
    transient int modCount;

    // threshold表示当HashMap的size大于threshold时会执行resize操作。
    // threshold=capacity*loadFactor
    int threshold;

    // 加载因子
    final float loadFactor;
```

##### 6.3.1.4 构造函数

```java
public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

```

##### 6.3.1.5 put方法

```java
		public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    static final int hash(Object key) {
        int h;
        // key的hash值的高16位和低16位进行异或
        // 是个扰动函数
        // 减少hash冲突
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Implements Map.put and related methods.
     *
     * @param hash hash for key key的hash值
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none 返回key先前对应的值
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
      
        // 1. table==null或者长度为0
        if ((tab = table) == null || (n = tab.length) == 0)
            // 初始化一个数组 默认是16
            n = (tab = resize()).length;
        // 2. (n - 1) & hash得到要放的数组的位置  没有hash冲突
        if ((p = tab[i = (n - 1) & hash]) == null)
            // 如果该位置是空的   则生成一个node对象 并在该位置放置该对象
            tab[i] = newNode(hash, key, value, null);
        else {
            // 3. 这个分支表示存在哈希冲突  
            Node<K,V> e; K k;
            // 哈希值相等 且对象相等或者equals方法返回true
            // 则表示需要覆盖
            // 3.1 hash值相同且(key相同或者equals方法相同)  则覆盖
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            // 当p是树节点时，把node放到红黑树中
            else if (p instanceof TreeNode)
                // 3.2 如果该节点是红黑树  则插入到红黑树
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 3.3 放到该位置的链表的尾部
                for (int binCount = 0; ; ++binCount) {
                    // 一直在遍历直到插入尾节点
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    // 遍历过程中去比较 哈希值相等 且对象相等或者equals方法是否相等
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            // 这里在覆盖
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        // 调整次数+1
        ++modCount;
        // 如果size+1后大于阈值 则扩容
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }
```

- resize()方法

```java
// 初始化map的size 或者扩容map
final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;
        // 旧的map的容量 即size
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        // 旧的阈值  
        int oldThr = threshold;
        int newCap, newThr = 0;
        // 1. 如果原map的长度大于0
        if (oldCap > 0) {
            // 1.1 如果旧的map容量大于最大容量  则返回旧的map
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            // 1.2 小于的话map的容量扩大到原来的两倍  阈值也扩大到两倍
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        // 2. 如果初始化时用户传入了容量参数  即oldThr大于0    那么将初始容量赋值给newCap
        else if (oldThr > 0) // initial capacity was placed in threshold
            // 初始容量为旧的阈值
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            // 如果什么都没传  将默认容量大小DEFAULT_INITIAL_CAPACITY(16)赋给newCap(新Node数组的长度)；
            newCap = DEFAULT_INITIAL_CAPACITY;
            // 将(默认负载因子)0.75f * (默认的容量大小)DEFAULT_INITIAL_CAPACITY的计算结果，赋值给newThr(新的扩容阀值)；
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        //如果初始化的时候用户传入了容量参数和负载因子或者只传入了容量参数，
	      //那么意味着oldCap==0、oldThr>0，那么上边的else里边是不会进入的，那么此时newThr(新的扩容阀值)仍然==0：
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }
```

##### 6.3.1.6 get方法

```java
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

   
    final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
        // 判断table是否为null  长度是否大于0 并且对应位置的值不等于空
        if ((tab = table) != null && (n = tab.length) > 0 &&
            (first = tab[(n - 1) & hash]) != null) {
            // 如果数组位置的哈希值相等且（key相等或者其equals方法相等） 则first
            if (first.hash == hash && 
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            // 否则去树节点或者链表节点去找
            if ((e = first.next) != null) {
                if (first instanceof TreeNode)
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }
```

#### 6.3.2 HashMap面试题

##### 6.3.2.1 为什么hashmap用数组+链表来实现

数组：查找快

链表：减小hash冲突

##### 6.3.2.2 为什么又加入了红黑树

在hash冲突时，防止链表过长，查找性能低时转化为红黑树

##### 6.3.2.3 为什么链表元素超过8时转化为红黑树

当==链表长度大于或者等于8时，且数组长度大于等于64时==，会把链表转化为红黑树

时间复杂度o(log(n))

为什么是8，看源码注释时的解释是因为泊松分布，链表长度符合泊松分布，各个长度概率命中率在8的时候，概率仅为0.00000006，所以在8的时候转换

防止用户自己的哈希算法不好导致链表过长，查询效率降低，是一种保底策略

##### 6.3.2.4 为什么红黑树节点个数小于6转化为链表

其实应该是小于8

当时这样==会造成链表和红黑树切换太过频繁==，所以中间有个差值7，故选择了6

##### 6.3.2.5 hashmap的hash怎么实现的

==对象的hashcode()高16位和低16位异或==，算是一个扰动函数，减小碰撞

##### 6.3.2.6 为什么要用异或运算符

保证了对象的 hashCode 的 32 位值==只要有一位发生改变==，整个==hash() 返回值就会改变==。尽可能的减少碰撞

##### 6.3.2.7 扩容过程

大于数组长度*加载因子时，会自动扩容

新数组代替容量小的数组，重新计算hash值

##### 6.3.2.8 hashmap中的modCount有什么作用

修改次数

迭代时不允许修改内容

一旦发现modCount与expectedModCount不一致，立即报错

抛出ConcurrentModificationException

##### 6.3.2.9 hashmap为啥线程不安全

Jdk1.7会造成环状链表，会造成死循环，引起CPU的100%问题

https://coolshell.cn/articles/9606.html
https://www.jianshu.com/p/1e9cf0ac07f4
https://www.jianshu.com/p/619a8efcf589
https://blog.csdn.net/loveliness_peri/article/details/81092360
https://cloud.tencent.com/developer/article/1120823
https://www.cnblogs.com/developer_chan/p/10450908.html

```java
void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    // 遍历数组
    for (Entry<K,V> e : table) {
        // 遍历链表
        while(null != e) {
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            // 头插法
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```

jdk1.8会造成数据覆盖，因为并发修改

##### 6.3.2.9 hashmap容量为啥是2的n次幂

只有当length=2的n次幂的时候，length-1的二进制表达全部为1（15的二进制1111，31的二进制位11111），只有当length-1的全部位都1时，h & (length-1)的结果才能==均匀散列在数组中==

### 6.4 ConcurrentHashMap原理

#### 6.4.1 jdk1.7与1.8的结构

- jdk1.7：ReentrantLock+Segment+HashEntry

- - jdk1.7的ConcurrentHashMap最外层是==多个segment==，每个==segment的底层数据结构与hashmap类似==，仍然是数组和链表组成的拉链法；
- - ==每个segment独立上了ReentrantLock锁==，每个segment之间互不影响，提高并发效率
  - 默认是有16个segment，所以最多同时支持16个线程并发读写，可以设置，但一旦初始化，segment不可以扩容

- jdk1.8：synchronized+CAS+HashEntry+红黑树
- - 已经接近了hashmap
  - ==JDK1.8的实现降低锁的粒度==，JDK1.7版本锁的粒度是基于Segment的，包含多个HashEntry，而JDK1.8锁的粒度就是HashEntry（首节点）
  - JDK1.8为什么使用内置锁synchronized来代替重入锁ReentrantLock，我觉得有以下几点
  - - 因为粒度降低了，==在相对而言的低粒度加锁方式，synchronized并不比ReentrantLock差==，在粗粒度加锁中ReentrantLock可能通过Condition来控制各个低粒度的边界，更加的灵活，而==在低粒度中，Condition的优势就没有了==
      ==JVM的开发团队从来都没有放弃synchronized，而且基于JVM的synchronized优化空间更大==，使用内嵌的关键字比使用API更加自然在大量的数据操作下，对于JVM的内存压力，基于API的ReentrantLock会开销更多的内存，虽然不是瓶颈，但是也是一个选择依据

#### 6.4.2 put方法

```java
//ConcurrentHashMap使用volatile修饰节点数组，保证其可见性，禁止指令重排。
transient volatile Node<K,V>[] table;

```

```java
 final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                // 1. 初始化table
                tab = initTable();
            // 如果table对应位置上的值不存在数据
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                // 2. cas操作 放置新节点
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            // 3. 如果table在扩容中  帮助其扩容
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                // 4. hash冲突
                V oldVal = null;
                synchronized (f) {
                    // 这里去判断数组中的该值是否还是f
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                           
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                               // 找到一个hash值相同 key也完全相同的节点  并更新
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                 // 否则在尾部插入一个节点
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
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
                }
                // 如果大于等于8 则树化
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }
```

```java
	  //table初始化   
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
		//将table赋值给tab，同时循环判断table是否为空，不为空说明已经有线程将table初始化了，则直接返回。
        while ((tab = table) == null || tab.length == 0) {
			//将sizeCtl(sizeControl)赋给sc，并判断是否<0,
            if ((sc = sizeCtl) < 0)
				//sc < 0说明有线程正在执行初始化操作，此时让出一下cpu，再次被调度后，继续执行while的判断。相当于等待的过程。
                Thread.yield(); // lost initialization race; just spin
			//如果sc>=0，则说明需要初始化，使用Cas的方式将sizeCtl赋值为-1，这样其他线程进来时就会走到上面的if中去。
			//根据返回值判断是否赋值成功，不成功的话，直接进行下一次循环，不成功的情况说明可能其他线程已经在初始化了。
			//Unsafe.compareAndSwapInt解释：
			//public final native boolean compareAndSwapInt(Object o, long offset, int expected, int x);
			//读取传入对象o在内存中偏移量为offset位置的值与期望值expected作比较,相等就把x值赋值给offset位置的值。方法返回true。不相等，就取消赋值，方法返回false。
			//具体到下面的if判断就是：
			//  检查ConcurrentHashMap对象在内存中偏移量为SIZECTL位置的int值（即为sizeCtl）与sc进行比较，相同就赋值为-1并返回true，不相等则取消赋值并返回false。
			//  SIZECTL是一个static final的常量，代表在当前ConcurrentHashMap对象中，sizeCtl变量在内存中的偏移量，private static final long SIZECTL;
			//  详见ConcurrentHashMap代码最后的static代码块
			//          U = sun.misc.Unsafe.getUnsafe();
			//			Class<?> k = ConcurrentHashMap.class;
			//			SIZECTL = U.objectFieldOffset(k.getDeclaredField("sizeCtl"));
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
				//赋值成功后进入
				//此处体现sizeCtl的一个含义，即sizeCtl = -1，说明正在有线程对table进行初始化。
                try {
					//再次赋值并判断tab是否为空，双重检查
					//防止当前线程在执行上面的if和else if判断期间，有其他线程已经完成Tab的初始化
                    if ((tab = table) == null || tab.length == 0) {
						//如果走到这，说明没有其他线程在对tab进行初始化，且在当前线程初始化完毕之前，不会有其他线程进来（通过sc < 0、U.compareAndSwapInt(this, SIZECTL, sc, -1)和双重检查实现）
						//此时sc可能>0，也可能=0，大于0则n赋值为sc，等于0则n赋值为table默认初始大小DEFAULT_CAPACITY=16。
						//sc > 0的情况，是调用构造方法时传入了tab的大小。
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
						
						//创建一个大小为n的Node<K,V>数组
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
						//新数组赋值给tab和table
                        table = tab = nt;
						// sc 赋值为n*0.75。
						// n>>>2 n无符号右移2位为原来的1/4(0.25)
						// n减掉n的1/4则为n*0.75,0.75为扩容因子。
                        sc = n - (n >>> 2);
                    }
                } finally {
					//sc赋值给sizeCtl
					//此处天sizeCtl的一个含义，即数组扩容的阈值。
                    sizeCtl = sc;
                }
				//不管当前线程有没有将table初始化，走到这里说明table已经被初始化完成了，可以跳出循环了
                break;
            }
        }
        return tab;
    }
```

#### 6.4.3 get方法

不加锁的

## 七、并发控制

### 7.1 等待线程完成-CountDownLatch类

在==完成一组正在其他线程中执行的操作之前==，`CountDownLatch`允许一个或者多个线程去==等待其他线程完成操作==。

```java
/**
 * 描述：     工厂中，质检，5个工人检查，所有人都认为通过，才通过
 */
public class CountDownLatchDemo1 {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        ExecutorService service = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            final int no = i + 1;
            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep((long) (Math.random() * 10000));
                        System.out.println("No." + no + "完成了检查。");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            };
            service.submit(runnable);
        }
        System.out.println("等待5个人检查完.....");
        latch.await();
        System.out.println("所有人都完成了工作，进入下一个环节。");
    }
}
```

```java
/**
 * 描述：     模拟100米跑步，5名选手都准备好了，只等裁判员一声令下，所有人同时开始跑步。
 */
public class CountDownLatchDemo2 {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch begin = new CountDownLatch(1);
        ExecutorService service = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            final int no = i + 1;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    System.out.println("No." + no + "准备完毕，等待发令枪");
                    try {
                        begin.await();
                        System.out.println("No." + no + "开始跑步了");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            service.submit(runnable);
        }
        //裁判员检查发令枪...
        Thread.sleep(5000);
        System.out.println("发令枪响，比赛开始！");
        begin.countDown();
    }
}

```

```java
/**
 * 描述：     模拟100米跑步，5名选手都准备好了，只等裁判员一声令下，所有人同时开始跑步。当所有人都到终点后，比赛结束。
 */
public class CountDownLatchDemo1And2 {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch begin = new CountDownLatch(1);

        CountDownLatch end = new CountDownLatch(5);
        ExecutorService service = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            final int no = i + 1;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    System.out.println("No." + no + "准备完毕，等待发令枪");
                    try {
                        begin.await();
                        System.out.println("No." + no + "开始跑步了");
                        Thread.sleep((long) (Math.random() * 10000));
                        System.out.println("No." + no + "跑到终点了");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        end.countDown();
                    }
                }
            };
            service.submit(runnable);
        }
        //裁判员检查发令枪...
        Thread.sleep(5000);
        System.out.println("发令枪响，比赛开始！");
        begin.countDown();

        end.await();
        System.out.println("所有人到达终点，比赛结束");
    }
}

```

### 7.2 控制并发线程数-许可证-Semaphore信号量

用来==限制或者管理数量有限的资源==的使用情况

维护一个==“许可证”计数==，线程==可以“获取”许可证==，则==信号量剩余的许可证就减1==，当==信号量所拥有的许可证数量为0==，那么==下一个还想要获取许可证的线程就需要等待==，直到另外的线程释放了许可证

```java
public class SemaphoreDemo {

    // true表示公平的
    static Semaphore semaphore = new Semaphore(5, true);

    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 100; i++) {
            service.submit(new Task());
        }
        service.shutdown();
    }

    static class Task implements Runnable {

        @Override
        public void run() {
            try {
                // 一次拿掉三个许可证
//                semaphore.acquire(3);
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "拿到了许可证");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "释放了许可证");
            
//            semaphore.release(2); // 一次释放两个许可证
            semaphore.release();
        }
    }
}

```

==拿了多少个许可证就应该释放多少个许可证==

### 7.3 锁对象监视器-Condition接口

任意一个Java对象，都拥有一组==监视器方法（定义在java.lang.Object上）==，主要==包括wait()、wait(long timeout)、notify()以及notifyAll()方法，这些方法与**synchronized同步关键字**配合==，可以实现==等待/通知模式==。Condition接口也==**提供了类似Object的监视器方法，与Lock配合可以实现等待/通知模式**==，但是这两者在使用方式以及功能特性上还是有差别的。

一个线程执行时，==检查条件是否满足，不满足的话调用`await()`方法==,自己阻塞在那；

第二个线程去把==创造条件，条件满足后调用`signal()`方法==来唤醒等待的线程

`signalAll()`会唤醒所有等待的线程

`signal()`是公平的，只会唤醒那个等待时间最长的线程

==是绑定在锁上的==，需要和锁配合使用

```java
/**
 * 描述：     演示Condition的基本用法 是个模板
 */
public class ConditionDemo1 {
    private ReentrantLock lock = new ReentrantLock();
    // 是绑定在锁上的
    private Condition condition = lock.newCondition();


    /**
     * 相当于主流程
     */
    void method1() throws InterruptedException {
        lock.lock();
        try{
            System.out.println("条件不满足，开始await");
            condition.await();
            System.out.println("条件满足了，开始执行后续的任务");
        }finally {
            lock.unlock();
        }
    }

    /**
     *  相当于准备线程
     */
    void method2() {
        lock.lock();
        try{
            System.out.println("准备工作完成，唤醒其他的线程");
            condition.signal();
        }finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ConditionDemo1 conditionDemo1 = new ConditionDemo1();
        // 开启准备线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    conditionDemo1.method2();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        // 这个线程必须写在之后 因为一旦卡死  主线程也卡死了
        conditionDemo1.method1();
    }
}

```

经典应用：生产者消费者模式

```java
/**
 * 描述：     演示用Condition实现生产者消费者模式
 */
public class ConditionDemo2 {

    private int queueSize = 10;
    private PriorityQueue<Integer> queue = new PriorityQueue<Integer>(queueSize);
    private Lock lock = new ReentrantLock();
    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition();

    public static void main(String[] args) {
        ConditionDemo2 conditionDemo2 = new ConditionDemo2();
        Producer producer = conditionDemo2.new Producer();
        Consumer consumer = conditionDemo2.new Consumer();
        producer.start();
        consumer.start();
    }

    class Consumer extends Thread {

        @Override
        public void run() {
            consume();
        }

        private void consume() {
            while (true) {
                lock.lock();
                try {
                    while (queue.size() == 0) {
                        System.out.println("队列空，等待数据");
                        try {
                            // 队列是空的时候  自己在那里wait
                            notEmpty.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // 消费
                    queue.poll();
                    // 告诉生产者生产
                    notFull.signalAll();
                    System.out.println("从队列里取走了一个数据，队列剩余" + queue.size() + "个元素");
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    class Producer extends Thread {

        @Override
        public void run() {
            produce();
        }

        private void produce() {
            while (true) {
                lock.lock();
                try {
                    while (queue.size() == queueSize) {
                        System.out.println("队列满，等待有空余");
                        try {
                            // 队列满了就等着
                            notFull.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // 否则生产
                    queue.offer(1);
                    // 并唤醒消费者
                    notEmpty.signalAll();
                    System.out.println("向队列插入了一个元素，队列剩余空间" + (queueSize - queue.size()));
                } finally {
                    lock.unlock();
                }
            }
        }
    }

}

```

==如果说lock用来代替synchronized，那么condition就是用来替代相对应的Object.wait/notify的==

condition的await方法会自动释放持有的lock锁，和Object.wait一样，不需要自己手动先释放锁

==调用await的时候，必须持有锁，否则会抛出异常，和Object.wait一样==

### 7.4 同步屏障-CycliBarrier

CycliBarrier循环栅栏和CountDownLatch很类似，都能阻塞一组线程

当有大量线程相互配合，==分别计算不同任务==，并且需要最后==统一汇总时==，我们可以使用CycliBarrier。CycliBarrier可以构造一个集合点，==直到所有的线程都到达集结点==，那么该==栅栏就会被撤销==，所有线程再统一出发，继续执行剩下的任务。

```java
/**
 * 描述：    演示CyclicBarrier
 */
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(5, new Runnable() {
            @Override
            public void run() {
                System.out.println("所有人都到场了， 大家统一出发！");
            }
        });
        for (int i = 0; i < 10; i++) {
            // 可重用  凑齐5个再触发
            new Thread(new Task(i, cyclicBarrier)).start();
        }
    }

    static class Task implements Runnable{
        private int id;
        private CyclicBarrier cyclicBarrier;

        public Task(int id, CyclicBarrier cyclicBarrier) {
            this.id = id;
            this.cyclicBarrier = cyclicBarrier;
        }

        @Override
        public void run() {
            System.out.println("线程" + id + "现在前往集合地点");
            try {
                // 前往过程中
                Thread.sleep((long) (Math.random()*10000));
                System.out.println("线程"+id+"到了集合地点，开始等待其他人到达");
                // 到了再等待  也就是说各个线程调用await方法就代表到了
                cyclicBarrier.await();
                System.out.println("线程"+id+"出发了");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}

```

CycliBarrier与CountDownLatch的区别

==**作用不同**==：CycliBarrier要等到==固定数量的线程都到达了栅栏位才能继续执行==；而CountDownLatch==只有等待数字到0==，也就是说==CountDownLatch用于事件，CycliBarrier用于线程==。

**==可重用性不同：==**CountDownLatch在倒数到0之后不能再使用，除非新建新的实例；而另一个则相反

## 八、AQS-AbstractQueuedSynchronizer：队列同步器

### 8.1 概述

队列同步器AbstractQueuedSynchronizer（以下简称同步器），是用来==构建锁或者其他同步组件的基础框架==，它使用了一个==int成员变量表示同步状态==，通过==内置的FIFO队列来完成资源获取线程的排队工作==，并发包的作者（Doug Lea）期望它能够成为实现大部分同步需求的基础。

同步器的主要使用方式是继承，==子类通过继承同步器并实现它的抽象方法来管理同步状态==，在抽象方法的实现过程中免不了要对同步状态进行更改，这时就需要使用同步器提供的3个方法（`getState()、setState(int newState)和compareAndSetState(int expect,int update)`）来进行操作，因为它们能够保证状态的改变是安全的。

==锁是面向使用者的==，它定义了使用者与锁交互的接口（比如可以允许两个线程并行访问），隐藏了实现细节；==同步器面向的是锁的实现者， 它简化了锁的实现方式，屏蔽了同步状态管理、线程的排队、等待与唤醒等底层操作==。锁和同步器很好地隔离了使用者和实现者所需关注的领域。

锁和协作类有共同点：==闸门==

- ReentrantLock和Semaphore、CountDownLatch、ReentrantReadWriteLock都有类似的“协作”的功能，其实，它们底层都用了一个共同的基类，AQS
- Semaphore内部有一个Sync类，Sync类继承了AQS

如果没有AQS需要每个协作工具自己实现：

- ==同步状态的原子性管理==
- ==线程的阻塞与解除阻塞==
- ==队列的管理==

在并发场景下，自己正确且高效实现这些内容，是相当有难度的，==AQS可以帮助我们搞定以上内容，我们只需要关注业务逻辑即可==。

AQS是一个用于==构建锁、同步器、协作工具类==的工具类。

AbstractQueuedSynchronizer是Doug Lea写的，**<font color=red>从JDK1.5加入的一个基于FIFO等待队列实现的一个用于实现同步器的基础框架</font>**，其实现类有以下：

![image-20221001224044377](/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20221001224044377.png)

AQS最核心的三大部分：

- ==state==
- - 这里的state的具体含义，会根据具体实现类的不同而不同，比如在==Semaphore==里，它表示“==剩余的许可证的数量==”，而在==CountDownLatch==里，它表示“==还需要倒数的数量==”。
  - state是volatile修饰，会被并发地修改，所以==所有修改state的方法都需要保证线程安全==，比如getState、setState以及compareAndSetState操作来读取和更新这个状态。这些方法都依赖于java.util.concurrent.atomic包的支持
  - 在ReentrantLock中，state用来表示“锁”的占有情况，包括可重入计数，当state的值为0的时候，标识为lock不为任何线程所占有
- 控制线程抢锁和配合的==FIFO队列==
- - 存放==“等待的线程”==，AQS是“排队管理器”，当多个线程争用同一把锁时，必须==有排队机制将那些没能拿到锁的线程串起来==。当锁释放时，锁管理器就会挑选一个合适的线程来占有刚释放的锁
- 期望协作工具类去实现的==获取/释放等重要方法==
- - 获取操作会依赖state变量，经常会阻塞（比如获取不到锁的时候）
  - 在Semaphoreh中，获取的就是acquire方法，作用是获取一个许可证
  - 在CountDownLatch里面，获取就是await方法，作用是“等待，直到倒数结束”

### 8.2 AQS

#### 8.2.0 主要方法

##### 1. 访问和修改同步状态方法

- `getState()`:==获取==当前同步状态的方法
- `setState(int newState)`：==设置当前同步状态==
- `compareAndSetState(int expect,int update)`：使用CAS设置当前状态，==该方法能够保证状态设置的原子性==。 

##### 2. 独占式/共享式获取和释放同步状态的方法

根据==是否独占来分为tryAcquire/tryRelease或tryAcquireShared(int acquires)和tryReleaseShared(int releases)==

##### 3.其他方法

#### 8.2.1 同步队列

内部数据结构node，实现同步队列

```java
static final class Node {
        // 共享模式下的节点
        static final Node SHARED = new Node();
        // 独占式的节点
        static final Node EXCLUSIVE = null;

        /** waitStatus value to indicate thread has cancelled */
        static final int CANCELLED =  1;
        /** waitStatus value to indicate successor's thread needs unparking */
        // 后继的节点处于等待状态
        static final int SIGNAL    = -1;
        /** waitStatus value to indicate thread is waiting on condition */
        // 节点在等待队列中，等待在condition上，当其他线程调用了signal()方法后
        // 该节点将会移动到同步队列中
        static final int CONDITION = -2;
        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         */
        // 表示下一次共享同步状态将会无条件传播下去
        static final int PROPAGATE = -3;

        // 等待状态
        volatile int waitStatus;

        volatile Node prev;

        volatile Node next;

        // 获取同步状态的线程
        volatile Thread thread;

        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         */
        Node nextWaiter;

 
    }
```

AQS中有一个头节点head

```java
 private transient volatile Node head;
```

同时有一个尾节点tail

```java
  private transient volatile Node tail;
```

首节点head：

- ==获取同步状态成功的节点==
- 在==释放同步状态时唤醒后继节点==，后继节点在==获取同步状态成功后将自己设置为头节点==

尾节点tail：

- 同步器提供了一个基于CAS的设置尾节点的方法：compareAndSetTail(Node expect,Node update)，它需要传递当前线程“认为”的尾节点和当前节点，只有设置成功后，当前节点才正式与之前的尾节点建立关联。 

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20221005013709667.png" alt="image-20221005013709667" style="zoom: 50%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20221005013822558.png" alt="image-20221005013822558" style="zoom: 50%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20221005013904272.png" alt="image-20221005013904272" style="zoom:50%;" />

#### 8.2.2 独占式获取与释放同步状态

```java
   public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
```

其中`tryAcquire`方法需要==自定义同步器==去实现，该方法时==保证线程安全的获取同步状态==

如果失败了，则调用`addWaiter`==将节点加入到同步队列的尾部==

```java

    //    上述代码通过使用compareAndSetTail(Node expect,Node update)方法来确保节点能够被线 程安全添加。
    //    试想一下：如果使用一个普通的LinkedList来维护节点之间的关系，
    //    那么当一个线 程获取了同步状态，而其他多个线程由于调用tryAcquire(int arg)方法获取同步状态失败而并发 地被添加到 LinkedList时，
    //    LinkedList将难以保证Node的正确添加，最终的结果可能是节点的数 量有偏差，而且顺序也是混乱的。
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // 当前认为的尾节点
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            // 只有设置成功了  当前节点和之前的尾部节点才能建立联系
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }

        enq(node);
        return node;
    }
   


    // 入队
    // 返回节点的前驱
    // 中，同步器通过“死循环”来保证节点的正确添加，在“死循 环”中只有通过CAS将节点设置成为尾节点之后，当前线程才能从该方法返回，否则，当前线 程不断地尝试设置。可以看出，enq(final Node node)方法将并发添加节点的请求通过CAS变 得“串行化”了。
    private Node enq(final Node node) {
        for (;;) {
           Node t = tail;
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
```

如果节点进入同步等待队列，那么将进入到==一个自旋的过程==,，每个节点（或者说每个线程）都在自省地观察，==当条件满足，获取到了同步状态，就可以从这个自旋过程中退出，否则依旧留在这个自旋过程中（并会阻塞节点的线程）==

```java
final boolean acquireQueued(Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                        parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

```

过调用同步器的release(int arg)方法可以释放同步状态，该方法在释放了同步状态之后，会唤醒其后继节点

```java
 public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }
```

独占式同步状态获取和释放总结：

- 在获取同步状态时，同步器维护一个同步队列，==获取状态失败的线程==都会被加入到队列中并在队列中进行==自旋==；
- 移出队列 （或停止自旋）的条件是==前驱节点为头节点且成功获取了同步状态==
- 在释放同步状态时，同步 器调用==`tryRelease(int arg)`方法释放同步状态，然后唤醒头节点的后继节点==。

#### 8.2.3 共享式获取与释放同步状态

```java
 public final void acquireShared(int arg) {
        // tryAcquireShared(int arg)方法返回值为int类型，当返回值大于等于0时，表示能够获取到同 步状态。
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
 }
```

```java
// 在doAcquireShared(int arg)方法的自 旋过程中，如果当前节点的前驱为头节点时，尝试获取同步状态，如果返回值大于等于0，表示 该次获取同步状态成功并从自旋过程中退出。
private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

```

#### 8.2.4 自定义同步组件-TwinsLock

设计一个同步工具：该工具在同一时刻，==只允许至多两个线程同时访问，超过两个线程的访问将被阻塞==，我们将这个同步工具命名为TwinsLock。 

==首先，确定访问模式==。TwinsLock能够在同一时刻支持多个线程的访问，这显然是共享式访问，因此，需要使用同步器提供的==acquireShared(int args)方法等和Shared相关的方法==，这就要求TwinsLock必须==重写tryAcquireShared(int args)方法和tryReleaseShared(int args)方法==，这样才能保证同步器的共享式同步状态的获取与释放方法得以执行。 

==其次，定义资源数==。TwinsLock在同一时刻允许至多两个线程的同时访问，表明同步资源数为2，这样可以设置初始状态status为2，当一个线程进行获取，status减1，该线程释放，则status加1，状态的合法范围为0、1和2，其中0表示当前已经有两个线程获取了同步资源，此时再有其他线程对同步状态进行获取，该线程只能被阻塞。==在同步状态变更时，需要使用compareAndSet(int expect,int update)方法做原子性保障==。 

最后，组合自定义同步器。前面的章节提到，自定义同步组件通过组合自定义同步器来完成同步功能，==一般情况下自定义同步器会被定义为自定义同步组件的内部类==。

```java
public class TwinsLock implements Lock {
    private final Sync sync = new Sync(2);

    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -7889272986162341211L;

        Sync(int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("count must large than zero.");
            }
            setState(count);
        }

        // cas操作变更同步状态  保证并发
        // 不然在这里自旋  是一个尝试获取共享锁的操作
        public int tryAcquireShared(int reduceCount) {
            for (;;) {
                int current = getState();
                int newCount = current - reduceCount;
                if (newCount < 0 || compareAndSetState(current, newCount)) {
                    return newCount;
                }
            }
        }

        // 同样 解锁的操作是个逆过程
        // 自旋
        public boolean tryReleaseShared(int returnCount) {
            for (;;) {
                int current = getState();
                int newCount = current + returnCount;
                if (compareAndSetState(current, newCount)) {
                    return true;
                }
            }
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }
    }

    public void lock() {
        sync.acquireShared(1);
    }

    public void unlock() {
        sync.releaseShared(1);
    }
  
  ...
}
```

#### 8.2.5 自定义一个独占锁-Mutex

```java
package com.dexlace.aqs;



import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 10-2
 */
public class Mutex implements Lock {
    // 静态内部类  自定义同步器
    private static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -4387327721959839431L;

        // 是否处于占用状态
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // lock的主要实现方法
        public boolean tryAcquire(int acquires) {
            // 断言  需要为1
            assert acquires == 1; // Otherwise unused
            // state为0时，cas，如果能设置成功表示上锁成功
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // unlock的主要方法
        protected boolean tryRelease(int releases) {
            // 断言  需要为1
            assert releases == 1;
            if (getState() == 0)
                throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        // 返回一个Condition，每个condition都包含了一个condition队列
        Condition newCondition() {
            return new ConditionObject();
        }
    }

    //  仅需要将操作代理到Sync上即可
    private final Sync sync = new Sync();

    public void lock() {
        sync.acquire(1);
    }

    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    public void unlock() {
        sync.release(1);
    }

    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}

```



### 8.3 AQS之于ReentrantLock详解

#### 8.3.1 Sync-继承aqs的抽象同步器

```java
  abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * lock()方法  FairSync与NonfairSync
         */
        abstract void lock();

       
        // tryAcquire方法需要在子类中实现  会调用这个非公平获取锁的方法
        @ReservedStackAccess
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            // 如果现在的state是0  则表示现在是第一次获取  不是重入
            if (c == 0) {
                // cas获取  获取成功了将会返回
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            // 如果当前同步器持有的线程是当前线程  则表示现在要冲突
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

    		// 释放锁的重入也类似
        @ReservedStackAccess
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
    
    
    
    
    
    
    
        // ...............
        // ...............

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }
```

#### 8.3.2 NonfairSync-基于非公平方式实现Sync同步器

```java
 static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        @ReservedStackAccess
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }
```

其中`acquire(int args)`方法调用的是`aqs`的`acquire(int args)`方法，即如下

```java
  @ReservedStackAccess
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&  // tryAcquire(arg)使用sync中的nonfairTryAcquire(acquires)来获取同步状态
            // 如果没有获取成功  则添加到同步队列  addWaiter即是这个作用 需要注意并发
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }
```

其中`tryAcquire(arg)`使用`NonfairSync`的`tryAcquire(int acquires) `方法，即

```java
 protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
 }
```

而`nonfairTryAcquire(acquires)`的使用`Sync`中的方法

#### 8.3.3 FairSync-基于公平方式实现Sync同步器

```java
static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        @ReservedStackAccess
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                // 没有正在排队的线程
                if (!hasQueuedPredecessors() &&
                    // 则设置为1
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }
```



#### 8.3.4 关于ReentrantLock的重入性

主要解决以下两个问题

- 线程**再次获取锁**。锁需要去==识别获取锁的线程是否为当前占据锁的线程==，如果==是，则再次成功获取==。 
- 锁的**最终释放**。线程重复n次获取了锁，随后在第n次释放该锁后，其他线程能够获取到该锁。锁的最终释放要求锁对于获取进行计数自增，计数表示当前锁被重复获取的次数，而锁被释放时，计数自减，当计数等于0时表示锁已经成功释放。 

```java
 final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
   					// 如果是0表示锁空闲 可以获取
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
   					// 如果是当前线程则计数加一
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
```

释放

```java
 protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            // 前n-1次释放都不会返回true  只有减为0才会释放
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
```

### 8.4 AQS之于CountDownLatch详解

#### 8.4.1 Sync-继承aqs的抽象同步器

```java
private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

  
        // 由于是共享同步  所以这个队列同步器需要实现的是shared结尾的方法
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        // 同理 且只有当减为0后才会返回  以起到所有等待的线程都执行完成才不阻塞的效果
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }
```

#### 8.4.2 await()方法

```java
  public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
  }
```

```java
 public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        // 除非tryAcquireShared(arg)小于0    
        //  如果此时state为0  才会有tryAcquireShared(arg)等于1  
        //  即表示count计数为0了  此时才不阻塞
        //  await()方法会立即返回
   
        // 如果state不为0  即计数不能减为0  则tryAcquireShared(arg)为-1 小于0
        // 会调用doAcquireSharedInterruptibly(arg)方法
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
 }
```

```java
  
// 共享同步状态的方法  这里在不停的自旋  自旋尝试获取同步状态
// 由aqs提供
private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }
```

所以对于CountDownLatch，其等待的方法只需要==重写`tryAcquireShared()`方法==和在`awiat()`方法中调用`acquireSharedInterruptibly(1)`即可

#### 8.4.3 countDown()方法

```java
 public void countDown() {
        sync.releaseShared(1);
 }
```

```java
 // aqs提供的
 public final boolean releaseShared(int arg) {
        // 所以也是只需要实现tryReleaseShared(arg)
        if (tryReleaseShared(arg)) {
            // 下面方法也是aqs提供的
            doReleaseShared();
            return true;
        }
        return false;
  }
```

也就是说`countDown`也只需要重写`tryReleaseShared(arg)`

### 8.5 AQS之用法总结

- 写一个类，把==协作的逻辑==想清楚，实现==获取/释放方法==
- 内部写一个Sync类==继承AbstractQueuedSynchronizer==
- 根据==是否独占来重写`tryAcquire/tryRelease`或`tryAcquireShared(int acquires)`和`tryReleaseShared(int releases)`==等方法

#### 8.5.1  AQS在CountDownLatch的应用总结

- 调用CountDownLatch的==await方法==时，便会尝试获取“共享锁”，不过一开始时获取不到该锁的，于是线程被阻塞。
- 而“共享锁”可获取到的条件，==就是“锁计数器“的值为0==
- ”锁计数器“的初始值是count，每当一个线程调用countDown()方法时，==才将“锁计数器”-1==
- count个线程调用countDown()之后，“锁计数器”才为0，而前面提到的等待获取共享锁的线程才能继续运行。

#### 8.5.2 AQS在Semaphore中的应用总结

- 在Semaphore中，==state表示许可证的剩余数量==
- 看tryAcquire方法，判断nonfairTryAcquireShared大于等于0的话，代表成功
- 检查==剩余许可证数量==，直接不够就返回负数，表示失败了；如果==够了就自旋加compareAndSetState来改变state状态，直到改变成功返回证书==；或是期间如果被其他修改了导致剩余数量不够了，那也返回负数达标获取失败

### 8.6 自定义一个一次性开闸放水的门闩

```java
package com.dexlace.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 描述：     自己用AQS实现一个简单的线程协作器。
 * OneShotLatch类的代码来自《Java并发编程实战》书
 * 一次性门闩
 */
public class OneShotLatch {

    private final Sync sync = new Sync();

    // 调用一次就放闸
    public void signal() {
        sync.releaseShared(0);
    }

    // 谁调用谁等待
    public void await() {
        sync.acquireShared(0);
    }

    private class Sync extends AbstractQueuedSynchronizer {

        // 1表示门闩打开 -1表示关闭
        // 1表示放行
        // 注意tryAcquireShared返回的参数为负数时会自旋等待
        @Override
        protected int tryAcquireShared(int arg) {
            return (getState() == 1) ? 1 : -1;
        }


        // 释放时将state置为1
        @Override
        protected boolean tryReleaseShared(int arg) {
           setState(1);

           return true;
        }
    }


    public static void main(String[] args) throws InterruptedException {
        OneShotLatch oneShotLatch = new OneShotLatch();
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName()+"尝试获取latch，获取失败那就等待");
                    oneShotLatch.await();
                    System.out.println("开闸放行"+Thread.currentThread().getName()+"继续运行");
                }
            }).start();
        }
        Thread.sleep(5000);
        // 开闸放水后不需要继续开闸  所以下面的这个线程继续运行
        oneShotLatch.signal();


        
        new Thread(new Runnable() {
            @Override
            public void run() {
                oneShotLatch.await();
                System.out.println("开闸放行"+Thread.currentThread().getName()+"继续运行");
            }
        }).start();
    }
}
```

美团技术团队《从ReentrantLock的实现看AQS的原理及应用》：https://mp.weixin.qq.com/s/sA01gxC4EbgypCsQt5pVog
老钱《打通 Java 任督二脉 —— 并发数据结构的基石》：https://juejin.im/post/5c11d6376fb9a049e82b6253
HongJie《一行一行源码分析清楚AbstractQueuedSynchronizer》：https://javadoop.com/post/AbstractQueuedSynchronizer
爱吃鱼的KK《AbstractQueuedSynchronizer 源码分析 (基于Java 8)》：https://www.jianshu.com/p/e7659436538b
waterystone《Java并发之AQS详解》：https://www.cnblogs.com/waterystone/p/4920797.html
英文论文的中文翻译：https://www.cnblogs.com/dennyzhangdd/p/7218510.html
AQS作者的英文论文：http://gee.cs.oswego.edu/dl/papers/aqs.pdf

## 九、线程池

### 9.1 线程池的创建

```java
// 最全参数的构造方法
ThreadPoolExecutor(int corePoolSize, 
                   int maximumPoolSize,
                   long keepAliveTime, 
                   TimeUnit milliseconds,
                   BlockingQueue<Runnable> workQueue,
                   ThreadFactory threadFactory, 
                   RejectedExecutionHandler handler);
```

#### 9.1.1 ==基本线程数corePoolSize==

当提交一个任务到线程池时，线程池会创建一个线程来执行任务，即使==其他空闲的基本线程能够执行新任务也会创建线程==，等到需要执行的==任务数大于线程池基本大小时就不再创建==。如果调用了==线程池的prestartAllCoreThreads()方法==， 线程池会提前==创建并启动所有基本线程==。 

#### 9.1.2 ==线程池最大数量maximumPoolSize==

线程池允许创建的最大线程数。如果==队列满了（肯定大于基本线程数），并且已创建的线程数小于最大线程数==，则线程池会再创建新的线程执行任务。值得注意的是，如果==使用了无界的任务队列这个参数就没什么效果==

#### 9.1.3 ==线程活动保持的时间keepAliveTime和单位TimeUnit==

==keepAliveTime（线程活动保持时间）==：线程池的工作线程==空闲后，保持存活的时间==。所以， 如果任务很多，并且每个任务执行的时间比较短，可以调大时间，提高线程的利用率。 

==TimeUnit（线程活动保持时间的单位）==：可选的==单位==有天（DAYS）、小时（HOURS）、分钟 （MINUTES）、毫秒（MILLISECONDS）、微秒（MICROSECONDS，千分之一毫秒）和纳秒（NANOSECONDS，千分之一微秒）。

#### 9.1.4 ==任务队列BlockingQueue<Runnable> workQueue==

用于==保存等待执行的任务的阻塞队列==。可以选择以下几 个阻塞队列。 

- ArrayBlockingQueue：是一个==基于数组结构的有界阻塞队列==，此队列按FIFO（先进先出）原 则对元素进行排序。 

- LinkedBlockingQueue：无解队列，一个基于链表结构的阻塞队列，此队列按FIFO排序元素，吞吐量通 常要高ArrayBlockingQueue。静态工厂方法Executors.newFixedThreadPool()使用了这个队列。 

- SynchronousQueue：==一个不存储元素的阻塞队列==。每个插入操作必须等到另一个线程调用 移除操作，否则插入操作一直处于阻塞状态，吞吐量通常要高于Linked-BlockingQueue，静态工 厂方法Executors.newCachedThreadPool使用了这个队列。 

- PriorityBlockingQueue：一个具有优先级的无限阻塞队列。

#### 9.1.5 线程工厂ThreadFactory

ThreadFactory：用于==设置创建线程的工厂==，可以通过线程工厂给每个创建出来的线程==设置更有意义的名字==。使用开源框架guava提供的ThreadFactoryBuilder可以快速给线程池里的线程设置有意义的名字，代码如下。

```java
new ThreadFactoryBuilder().setNameFormat("XX-task-%d").build();
```

#### 9.1.6 ==饱和策略RejectedExecutionHandler==

当队列和线程池都满了，说明线程池处于饱和状态，那么必须采取一种策略处理提交的新任务。这个策略默认情况下是AbortPolicy，表示无法处理新任务时抛出异常。在JDK 1.5中Java线程池框架提供了以下4种策略。 

- ==AbortPolicy：直接抛出异常==。 

- CallerRunsPolicy：只用调用者所在线程来运行任务。 

- DiscardOldestPolicy：丢弃队列里最近的一个任务，并执行当前任务。 

- DiscardPolicy：不处理，丢弃掉。 

当然，也可以根据应用场景需要来实现RejectedExecutionHandler接口自定义策略。如记录 日志或持久化存储不能处理的任务。 

### 9.2 线程池的实现原理

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602142225628.png" alt="image-20210602142225628" style="zoom:67%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602142332671.png" alt="image-20210602142332671" style="zoom:67%;" />

### 9.3. Executor框架的结构

- 任务：需要实现Runnable接口或者Callable接口
- 执行单元：==核心接口Executor==以及==继承自Executor的ExecutorService接口==，实现ExecutorService接口的实现类：==<font color=red>ThreadPoolExecutor</font>==和==<font color=red>ScheduledThreadPoolExecutor</font>==
- 异步计算的结果：包括==接口Future==和实现==Future接口的FutureTask类==。

### 9.4. Executor框架的成员

#### 9.4.1 ThreadPoolExecutor

ThreadPoolExecutor通常使用==<font color=red>工厂类Executors</font>==来创建。Executors可以创建3种类型的ThreadPoolExecutor：==SingleThreadExecutor、FixedThreadPool==和==CachedThreadPool==

##### 1. ==FixedThreadPool==

- ==<font color=red>FixedThreadPool</font>==

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602151223099.png" alt="image-20210602151223099" style="zoom:67%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602152119300.png" alt="image-20210602152119300" style="zoom:67%;" />

**<font color=red>核心线程数和最大线程数相等</font>**，只要它们满了，就要到队列待了，空闲线程保持存活时间为0，一旦执行完就没了。(==这里把keepAliveTime设置为0L，意味着多余 的空闲线程会被立即终止==)。后续只能新创建线程补充。

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602155918227.png" alt="image-20210602155918227" style="zoom:50%;" />



FixedThreadPool适用于==资源管理的需求==，而需要==限制当前线程数量的应用场景==，它适用于==负载比较重的服务器==

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602160343662.png" alt="image-20210602160343662" style="zoom: 67%;" />

由于传进来的LinkedBlockingQueue是没有容量上限的，所以当请求数越来越多，==<font color=red>并且无法及时处理完毕时，即请求堆积时，会造成占用大量的内存，可能会导致OOM。</font>==

```java
/**
 * 描述：     演示newFixedThreadPool出错的情况
 */
public class FixedThreadPoolOOM {

    private static ExecutorService executorService = Executors.newFixedThreadPool(1);
    public static void main(String[] args) {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            executorService.execute(new SubThread());
        }
    }

}

class SubThread implements Runnable {


    @Override
    public void run() {
        try {
            Thread.sleep(1000000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

##### 2. ==<font color=red>SingleThreadExecutor</font>==

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602152315233.png" alt="image-20210602152315233" style="zoom:67%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602152621951.png" alt="image-20210602152621951" style="zoom:67%;" />

SingleThreadExecutor适用于==需要保证顺序地执行各个任务==；并且在任意时间点，不会有多 个线程是活动的应用场景。

==其实是FixedThreadPool的特例，核心线程数和最大线程数为1==

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602160553067.png" alt="image-20210602160553067" style="zoom:67%;" />

```java
public class SingleThreadExecutor {
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 1000; i++) {
            executorService.execute(new Task());
        }
    }
}
```

##### 3. ==<font color=red>CachedThreadPool</font>==

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602152953777.png" alt="image-20210602152953777" style="zoom:67%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602153016766.png" alt="image-20210602153016766" style="zoom:67%;" />

CachedThreadPool是==大小无界的线程池==，适用于==执行很多的短期异步任务的小程序==，或者是==负载较轻的服务器==。 

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602161004681.png" alt="image-20210602161004681" style="zoom:67%;" />

#### 9.4.2 ScheduledThreadPoolExecutor 

ScheduledThreadPoolExecutor通常使用==工厂类Executors==来创建。

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602153644963.png" alt="image-20210602153644963" style="zoom:67%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602153714992.png" alt="image-20210602153714992" style="zoom:67%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602153756008.png" alt="image-20210602153756008" style="zoom:67%;" />

ScheduledThreadPoolExecutor适用于==需要多个后台线程执行周期任务==，同时为了满足资源管理的需求而需要限制后台线程的数量的应用场景

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20210602154229431.png" alt="image-20210602154229431" style="zoom:67%;" />

SingleThreadScheduledExecutor适用于需要==单个后台线程执行周期任务==，同时需要保证顺序地执行各个任务的应用场景。

#### 9.4.3 Future接口

==Future接口==和实现Future接口的==FutureTask类==用来表示==异步计算的结果==。当我们把==Runnable 接口==或==Callable接口==的实现类提交<font color=red>（**submit）**</font>给ThreadPoolExecutor或 ScheduledThreadPoolExecutor时，ThreadPoolExecutor或ScheduledThreadPoolExecutor会向我们返回一个FutureTask对象。下面是对应的API。 

```java
<T> Future<T> submit(Callable<T> task) 
<T> Future<T> submit(Runnable task, T result) 
    Future<> submit(Runnable task)
```

#### 2.4 Runnable接口和Callable接口

Runnable接口和Callable接口的实现类，都可以被==ThreadPoolExecutor==或==ScheduledThreadPoolExecutor==执行。它们之间的区别是==Runnable不会返回结果==，而==Callable可以返回结果==。

除了可以自己创建实现Callable接口的对象外，还可以使用==<font color=red>工厂类Executors</font>来把一个<font color=red>Runnable包装成一个Callable</font>。== 

```java
public static Callable<Object> callable(Runnable task) // 假设返回对象Callable1
```

```java
public static <T> Callable<T> callable(Runnable task, T result)  // 假设返回对象Callable2
```

==submit（…）会向我们返回一个FutureTask对象==。我们可以执行==FutureTask.get()方法来等待任务执行完成==。当任务成功完成后 

==FutureTask.get()将返回该任务的结果。==

例如，如果提交的是对象Callable1，==FutureTask.get()方法将返回null==；

如果提交的是对象Callable2，==FutureTask.get()方法将返回result对象==。

### 9.5 线程池线程数量设置

<img src="/Users/dexlace/private-github-repository/xmind-all/01_multiThread/并发final.assets/image-20221009154605637.png" alt="image-20221009154605637" style="zoom:50%;" />