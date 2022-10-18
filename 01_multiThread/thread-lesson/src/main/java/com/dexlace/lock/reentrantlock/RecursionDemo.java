package com.dexlace.lock.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

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
