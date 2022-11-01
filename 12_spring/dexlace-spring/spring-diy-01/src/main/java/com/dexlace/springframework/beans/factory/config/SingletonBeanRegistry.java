package com.dexlace.springframework.beans.factory.config;


/**
 * bean的注册中心
 */
public interface SingletonBeanRegistry {

    Object getSingleton(String beanName);

}
