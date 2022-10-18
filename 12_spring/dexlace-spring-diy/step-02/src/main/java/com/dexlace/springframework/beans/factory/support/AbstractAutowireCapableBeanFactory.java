package com.dexlace.springframework.beans.factory.support;


import com.dexlace.springframework.beans.BeansException;
import com.dexlace.springframework.beans.factory.factory.BeanDefinition;


/**
 * 创建bean并放入缓存池
 */
public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory {

    @Override
    protected Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException {
        Object bean = null;
        try {
            bean = beanDefinition.getBeanClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BeansException("Instantiation of bean failed", e);
        }

        addSingleton(beanName, bean);
        return bean;
    }

}
