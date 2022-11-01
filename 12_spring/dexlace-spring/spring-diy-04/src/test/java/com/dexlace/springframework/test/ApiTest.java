package com.dexlace.springframework.test;


import com.dexlace.springframework.beans.PropertyValue;
import com.dexlace.springframework.beans.PropertyValues;
import com.dexlace.springframework.beans.factory.config.BeanDefinition;
import com.dexlace.springframework.beans.factory.config.BeanReference;
import com.dexlace.springframework.beans.factory.support.DefaultListableBeanFactory;
import com.dexlace.springframework.test.bean.UserDao;
import com.dexlace.springframework.test.bean.UserService;
import org.junit.Test;


public class ApiTest {

    @Test
    public void test_BeanFactory() {
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2. UserDao 注册
        beanFactory.registerBeanDefinition("userDao", new BeanDefinition(UserDao.class));

        // 3. UserService 设置属性[uId、userDao]
        PropertyValues propertyValues = new PropertyValues();
        // 普通的value
        propertyValues.addPropertyValue(new PropertyValue("uId", "10001"));
        // bean的话只给一个bean的引用
        propertyValues.addPropertyValue(new PropertyValue("userDao",new BeanReference("userDao")));

        // 4. UserService 注入bean
        // 连带着他的属性
        BeanDefinition beanDefinition = new BeanDefinition(UserService.class, propertyValues);
        beanFactory.registerBeanDefinition("userService", beanDefinition);

        // 5. UserService 获取bean
        UserService userService = (UserService) beanFactory.getBean("userService");
        userService.queryUserInfo();
    }

}
