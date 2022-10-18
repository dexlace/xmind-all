package com.dexlace.threadlocal.version2;




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
        x.set(109L); // 假设这一行执行 则main线程的初始化方法也不会跑了
        // 运行结果 两个线程id不一样
        System.out.println("main get: "+x.get());
    }
}
