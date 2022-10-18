package com.dexlace.springframework.beans.factory;


import com.dexlace.springframework.beans.BeansException;


/**
 * 生产bean的工厂  生产的功能
 * 获取bean和生产bean
 *
 * 所以之后的实现类会实现
 *
 * 1。缓存bean的接口
 * 2。生产bean的接口
 * 3。最终的实现类还有缓存beanDefinition的接口
 */
public interface BeanFactory {

    Object getBean(String name) throws BeansException;

}
