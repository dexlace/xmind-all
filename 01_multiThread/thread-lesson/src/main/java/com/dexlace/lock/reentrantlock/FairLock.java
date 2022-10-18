package com.dexlace.lock.reentrantlock;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 描述：     演示公平和不公平两种情况
 */
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

    private Lock queueLock = new ReentrantLock(false);

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

        // 打完第一份工 再来排队
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
