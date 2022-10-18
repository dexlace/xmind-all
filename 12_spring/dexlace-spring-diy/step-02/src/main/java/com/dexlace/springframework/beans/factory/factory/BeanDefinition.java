package com.dexlace.springframework.beans.factory.factory;

/**
 * 一、一开始你有个beanDefinition对象包装class
 *
 */
public class BeanDefinition {

    private Class beanClass;

    public BeanDefinition(Class beanClass) {
        this.beanClass = beanClass;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }
}
