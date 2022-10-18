package com.dexlace.flowcontrol.condition;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 描述：     演示Condition的基本用法
 */
public class ConditionDemo1 {
    private ReentrantLock lock = new ReentrantLock();
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
