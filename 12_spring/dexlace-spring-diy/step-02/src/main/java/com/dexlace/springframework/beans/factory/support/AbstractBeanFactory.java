package com.dexlace.springframework.beans.factory.support;


import com.dexlace.springframework.beans.BeansException;
import com.dexlace.springframework.beans.factory.BeanFactory;
import com.dexlace.springframework.beans.factory.factory.BeanDefinition;


/**
 * 主线就是如何去获取bean  不存在时去生成bean
 * getBean去缓存获取  或者创建完获取
 * 加载过程会去找缓存是否会会获取 如果没有  则去创建
 *
 *
 */
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {

    @Override
    public Object getBean(String name) throws BeansException {
        Object bean = getSingleton(name);
        if (bean != null) {
            return bean;
        }

        BeanDefinition beanDefinition = getBeanDefinition(name);
        return createBean(name, beanDefinition);
    }

    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException;

}
