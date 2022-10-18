package com.dexlace.springframework.beans.factory.factory;

/**
 * 单例对象的注册表
 * 单一职责：注册或者说缓存
 */
public interface SingletonBeanRegistry {

    Object getSingleton(String beanName);

}
