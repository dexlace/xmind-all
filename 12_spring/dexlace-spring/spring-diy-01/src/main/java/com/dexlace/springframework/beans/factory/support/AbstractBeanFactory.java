package com.dexlace.springframework.beans.factory.support;


import com.dexlace.springframework.beans.BeansException;
import com.dexlace.springframework.beans.factory.BeanFactory;
import com.dexlace.springframework.beans.factory.config.BeanDefinition;

/***
 * 只实现从注册中心中拿bean
 */
public abstract class AbstractBeanFactory extends DefaultSingletonBeanRegistry implements BeanFactory {

    @Override
    public Object getBean(String name) throws BeansException {
        Object bean = getSingleton(name);
        if (bean != null) {
            return bean;
        }

        // 缓存拿不到bean  去beandefinitionmap拿对应的beandefinition
        // 拿到去创建bean
        BeanDefinition beanDefinition = getBeanDefinition(name);
        return createBean(name, beanDefinition);
    }

    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract Object createBean(String beanName, BeanDefinition beanDefinition) throws BeansException;

}
