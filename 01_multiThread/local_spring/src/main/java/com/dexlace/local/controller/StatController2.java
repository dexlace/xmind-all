package com.dexlace.local.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;


// 本方案不行  integer
@RestController
public class StatController2 {

    static HashSet<Integer> set=new HashSet<>();

    static  synchronized  void addSet(Integer v){
        set.add(v);
    }
    static  ThreadLocal<Integer> c= new ThreadLocal<Integer>(){
        @Override
        protected Integer initialValue() {
            // 所以每个线程第一次调用get将会触发一次
            // 所以这里set的size等于收集的线程数
            Integer val= 0;
            // 临界区  会有同步问题  需要加锁
            // 由于每个线程只执行一次 所以效率问题不大
//            set.add(val);
            addSet(val);
            return val;
        }
    };


    @RequestMapping("/stat2")
    public Integer stat(){
        // 汇总多个线程的结果
        return  set.stream().reduce(Integer::sum).get();

    }


    @RequestMapping("/add2")
    public Integer add() throws InterruptedException {
        Thread.sleep(100);
        // 拿到当前的线程的val
        // 所以每个线程第一次调用get将会触发一次初始化方法
        c.set(c.get()+1);
        return 1;
    }

}
