package com.dexlace.springframework.beans.factory.support;



import com.dexlace.springframework.beans.factory.factory.SingletonBeanRegistry;

import java.util.HashMap;
import java.util.Map;


/**
 * 单例bean的注册中心
 *  bean的缓存
 * 同时实现了一个受保护的 addSingleton 方法，这个方法可以被继承此类的其他类调用
 * 只有一个功能：注册，或者说缓存
 *
 * 所以说这是一个bean的缓存池
 */
public class DefaultSingletonBeanRegistry implements SingletonBeanRegistry {

    private Map<String, Object> singletonObjects = new HashMap<>();

    @Override
    public Object getSingleton(String beanName) {
        return singletonObjects.get(beanName);
    }

    protected void addSingleton(String beanName, Object singletonObject) {
        singletonObjects.put(beanName, singletonObject);
    }

}
