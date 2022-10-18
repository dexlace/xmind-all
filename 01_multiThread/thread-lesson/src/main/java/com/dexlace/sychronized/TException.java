package com.dexlace.sychronized;

import java.util.concurrent.TimeUnit;

// 演示出现异常时锁是否释放
public class TException {
    int count=0;
    synchronized void m(){
        System.err.println(Thread.currentThread().getName()+" start");
        while(true){
            count++;
            System.err.println(Thread.currentThread().getName()+" count="+count);

            try {
                TimeUnit.SECONDS.sleep(1);
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            // 抛出ArithmeticException
            if (count==5){
                int i=1/0;
            }
        }
    }

    public static void main(String[] args) {
        TException tException=new TException();
        new Thread(tException::m,"t1").start();
        try {
            TimeUnit.SECONDS.sleep(5);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        // 运行得知，t1线程出现了by zero异常，结果释放了锁  t2得到了执行
        // 所以如需避免此情况，请做好异常处理
        new Thread(tException::m,"t2").start();
    }
}
