package com.dexlace.springframework.beans.factory.support;


import com.dexlace.springframework.beans.BeansException;
import com.dexlace.springframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Constructor;



// 定义实例化策略
public interface InstantiationStrategy {

    Object instantiate(BeanDefinition beanDefinition, String beanName, Constructor ctor, Object[] args) throws BeansException;

}
