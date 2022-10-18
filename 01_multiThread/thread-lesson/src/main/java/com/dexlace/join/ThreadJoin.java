package com.dexlace.join;

import org.omg.CORBA.PRIVATE_MEMBER;

import java.util.concurrent.TimeUnit;

// join方法
public class ThreadJoin {

    public static void main(String[] args) throws Exception{
        Thread previous=Thread.currentThread();
        for (int i=0;i<5;i++){
            // 每个线程拥有前一个线程的引用，需要等待前一个线程终止，才能从等待中返回
             Thread thread = new Thread(new Domino(previous), String.valueOf(i));
             thread.start();
             previous = thread;
        }
        TimeUnit.SECONDS.sleep(5);
        System.err.println(Thread.currentThread().getName() + " terminate.");
    }

   static class Domino implements Runnable{
        private Thread thread;
        public Domino(Thread thread){
            this.thread=thread;
        }

        @Override
        public void run() {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println(Thread.currentThread().getName() + " terminate");
        }
    }

}
