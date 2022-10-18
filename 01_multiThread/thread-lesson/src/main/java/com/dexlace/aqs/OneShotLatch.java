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
