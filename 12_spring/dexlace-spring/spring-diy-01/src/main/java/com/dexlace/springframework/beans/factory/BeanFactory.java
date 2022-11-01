package com.dexlace.springframework.beans.factory;


import com.dexlace.springframework.beans.BeansException;

public interface BeanFactory {

    Object getBean(String name) throws BeansException;

}
