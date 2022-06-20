# volatile   

## 一、volatile关键字概览

## 1.1 多线程下变量的不可见性

### 1.1.1 概述

​         在多线程并发执行下，多个线程修改共享的成员变量，会出现一个线程修改了共享变量的值后，另一个线程不能直接看到该线程修改后的变量的最新值。

### 1.1.2 案例演示

```java
package com.dexlace.juc.visible;

/**
    目标：研究一下多线程下变量访问的不可见性现象。

    准备内容：
        1.准备2个线程。
        2.定义一个成员变量。
        3.开启两个线程，其中一个线程负责修改，另外一个负责读取。
 */
public class VisibilityDemo01 {
    // main方法，作为一个主线程。
    public static void main(String[] args) {
        // a.开启一个子线程
        MyThread t = new MyThread();
        t.start();

        // b.主线程执行
        while(true){
            if(t.isFlag()){
                System.out.println("主线程进入循环执行~~~~~");
            }
        }
    }
}

class MyThread extends Thread{
    // 成员变量
    private boolean flag = false;
    @Override
    public void run() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 触发修改共享成员变量
        flag = true;
        System.out.println("flag="+flag);
    }
    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}

```

### 1.1.3 执行结果

![image-20220611230304238](volatile.assets/image-20220611230304238.png)

我们可以发现，==子线程已经将flag设置为true，但main方法中始终没有读到修改后的最新值==，从而循环没有进入到if语句中执行，所以没有任何打印。

### 1.1.4 小结

多线程下修改共享变量会出现变量修改值后的不可见性。