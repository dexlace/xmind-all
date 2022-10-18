package com.dexlace.springframework.beans.factory.support;


import com.dexlace.springframework.beans.factory.factory.BeanDefinition;


/**
 * beanDefinition的注册 缓存
 */
public interface BeanDefinitionRegistry {

    /**
     * 向注册表中注册 BeanDefinition
     */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);

}
