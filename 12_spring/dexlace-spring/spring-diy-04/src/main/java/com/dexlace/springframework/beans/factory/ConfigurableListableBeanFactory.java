package com.dexlace.springframework.beans.factory;


import com.dexlace.springframework.beans.BeansException;
import com.dexlace.springframework.beans.factory.config.AutowireCapableBeanFactory;
import com.dexlace.springframework.beans.factory.config.BeanDefinition;
import com.dexlace.springframework.beans.factory.config.ConfigurableBeanFactory;

public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

    BeanDefinition getBeanDefinition(String beanName) throws BeansException;

}
