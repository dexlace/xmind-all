package com.dexlace.wait;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WaitNotify {

    static boolean flag=true;
    static Object object =new Object();

    public static void main(String[] args) throws Exception {
        Thread waitThread=new Thread(new Wait(),"WaitThread");
        waitThread.start();
        TimeUnit.SECONDS.sleep(1);
        Thread notifyThread=new Thread(new Notify(),"NotifyThread");
        notifyThread.start();
    }



    static class Wait implements Runnable{
        @Override
        public void run() {
            // 加锁 拥有lock的monitor
            synchronized (object){
                // 条件不满足时，继续wait，同时释放了lock的锁
                while(flag){
                    try{
                        System.err.println(Thread.currentThread() + " flag 为true，等待中@"
                                + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                        object.wait();
                    }catch (InterruptedException e){

                    }
                }

                // 条件满足时 完成工作
                System.err.println(Thread.currentThread() + " flag 为false. 继续运行@"
                        + new SimpleDateFormat("HH:mm:ss").format(new Date()));
            }

        }
    }

    static class Notify implements Runnable{
        @Override
        public void run() {
            synchronized (object){
                // 获取锁 然后进行通知 通知时不会释放lock的锁
                // 直到当前线程释放了锁  waitThread才能从wait方法中返回
                System.out.println(Thread.currentThread() + " 持有锁，通知@"
                        + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                object.notifyAll();
                flag=false;
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            // 再次加锁
            synchronized (object){
                System.out.println(Thread.currentThread() + " 再次持有锁，通知@"
                        + new SimpleDateFormat("HH:mm:ss").format(new Date()));
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
