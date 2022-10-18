
package com.dexlace.springframework.beans.factory;

import com.dexlace.springframework.beans.BeansException;

public interface BeanFactory {

    Object getBean(String name) throws BeansException;


    // 重载了一个含有入参信息 args 的 getBean 方法，这样就可以方便的传递入参给构造函数实例化了
    Object getBean(String name, Object... args) throws BeansException;

}
