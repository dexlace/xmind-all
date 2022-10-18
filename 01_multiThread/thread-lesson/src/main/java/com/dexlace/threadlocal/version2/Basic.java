package com.dexlace.threadlocal.version2;




public class Basic {

    public static ThreadLocal<Long> x=new ThreadLocal<Long>(){
        @Override
        protected Long initialValue() {
            System.out.println("初始化方法将会执行一次");
            return 100L;
        }
    };

    public static void main(String[] args) {
        System.out.println(x.get());
        System.out.println(x.get());
    }
}
