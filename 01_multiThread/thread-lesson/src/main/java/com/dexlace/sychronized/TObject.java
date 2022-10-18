package com.dexlace.sychronized;

import java.util.concurrent.TimeUnit;

public class TObject {

    Object o=new Object();

    void m(){
        synchronized (o){
            while(true){
                System.err.println(Thread.currentThread().getName()+"运行中");
                try {
                    TimeUnit.SECONDS.sleep(1);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        TObject t=new TObject();
        new Thread(t::m,"thread 1").start();

        try {
            TimeUnit.SECONDS.sleep(3);
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        Thread thread2=new Thread(t::m,"thread 2");
        // 改变锁的引用，线程2也有机会运行
        t.o=new Object();
        thread2.start();
    }
}
