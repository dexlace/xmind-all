package com.dexlace.sychronized;

// 演示不要以字符串常量作锁定对象
public class TString {
    // m1和m2锁的其实是同一对象
    String s1="hello";
    String s2="hello";

    void m1(){
        synchronized (s1){

        }
    }

    void m2(){
        synchronized (s2){

        }
    }
}
