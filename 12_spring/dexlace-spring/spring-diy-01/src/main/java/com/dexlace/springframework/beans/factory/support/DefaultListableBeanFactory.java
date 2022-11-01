package com.dexlace.springframework.beans.factory.support;


import com.dexlace.springframework.beans.BeansException;
import com.dexlace.springframework.beans.factory.config.BeanDefinition;


import java.util.HashMap;
import java.util.Map;


/**
 * 生产bean  添加到注册中心
 * 并继承从bean注册中心获取bean的功能
 * 同时有注册beandefinition的功能
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements BeanDefinitionRegistry {

    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) throw new BeansException("No bean named '" + beanName + "' is defined");
        return beanDefinition;
    }

}
