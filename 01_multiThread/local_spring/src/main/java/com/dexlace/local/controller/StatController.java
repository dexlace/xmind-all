package com.dexlace.local.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;

@RestController
public class StatController {

    static HashSet<Val<Integer>> set=new HashSet<>();

    static  synchronized  void addSet(Val<Integer> v){
        set.add(v);
    }
    static  ThreadLocal<Val<Integer>> c= new ThreadLocal<Val<Integer> >(){
        @Override
        protected Val<Integer> initialValue() {
            // 所以每个线程第一次调用get将会触发一次
            // 所以这里set的size等于收集的线程数
            Val<Integer> val=new Val<>();
            val.set(0);
            // 临界区  会有同步问题  需要加锁
            // 由于每个线程只执行一次 所以效率问题不大
//            set.add(val);
            addSet(val);
            return val;
        }
    };


    @RequestMapping("/stat")
    public Integer stat(){
        // 汇总多个线程的结果
        return  set.stream().map(Val::get).reduce(Integer::sum).get();

    }


    @RequestMapping("/add")
    public Integer add() throws InterruptedException {
        Thread.sleep(100);
        // 拿到当前的线程的val
        // 所以每个线程第一次调用get将会触发一次初始化方法
        Val<Integer> v=c.get();
        v.set(v.get()+1);
        return 1;
    }

}
