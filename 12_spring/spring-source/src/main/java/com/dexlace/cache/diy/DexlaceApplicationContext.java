package com.dexlace.cache.diy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DexlaceApplicationContext {

    private Map<String, BeanDefinition> beanDefinitionMap=new LinkedHashMap<>();

    // 一级缓存 储存所有完整的bean
    private final Map<String,Object> singletonObjects=new ConcurrentHashMap<>();

    // 二级缓存 为了解决循环依赖  并且能够解决并发下获取不完整bean 性能问题
    // 不成熟的bean
    private final Map<String,Object> earlySingletonObjects=new ConcurrentHashMap<>();


    public DexlaceApplicationContext() throws Exception{
        // 加载ioc容器  创建所有的bean
        refresh();
    }

    private void refresh()  throws Exception{
        loadBeanDefinitions();

        // 创建所有的单例bean
        finishBeanFactoryInitialization();
    }

    private void finishBeanFactoryInitialization()  throws Exception{
        for (String beanName : beanDefinitionMap.keySet()) {
            getBean(beanName);

        }
    }

    public Object getBean(String beanName) throws Exception{
        // 判断bean是否创建好
        // 如果已经创建好  直接返回
        Object bean = getSingleton(beanName);
        if (bean!=null){
            return bean;
        }
        // 没有创建则开始创建
        // 加锁是为了防止创建多个bean
        synchronized (singletonObjects) {
            // 双重检查 防止重复创建bean及一级缓存
            if (singletonObjects.containsKey(beanName)){
                return singletonObjects.get(beanName);
            }

        // 1. 实例化  -- 反射
        RootBeanDefinition definition = (RootBeanDefinition)beanDefinitionMap.get(beanName);
        Class<?> beanClass = definition.getBeanClass();
        bean = beanClass.newInstance();

        // 存二级缓存  存储不完整的bean  未成熟的bean
        earlySingletonObjects.put(beanName,bean);
        // 2. 属性注入
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Autowired annotation = declaredField.getAnnotation(Autowired.class);
            if (annotation!=null){
                // 根据名字进行注入
                String name = declaredField.getName();
                Object depBean = getBean(name);

                declaredField.setAccessible(true);
                // 给bean对象设置属性
                declaredField.set(bean,depBean);

            }


        }
        // 3. 初始化 ... aop动态代理

        // 4. 放入一级缓存  一级缓存储存了成熟的bean
            singletonObjects.put(beanName,bean);
        earlySingletonObjects.remove(beanName);

        return bean;
        }
    }

    private Object getSingleton(String beanName){
        if (singletonObjects.containsKey(beanName)){
            return singletonObjects.get(beanName);
        }

        synchronized (earlySingletonObjects) {
            if (earlySingletonObjects.containsKey(beanName))
                return earlySingletonObjects.get(beanName);
        }


        return null;

    }

    // 加载bean的信息 动态创建bean
    private void loadBeanDefinitions() {

        RootBeanDefinition aBeanDefinition = new RootBeanDefinition(A.class);

        RootBeanDefinition bBeanDefinition = new RootBeanDefinition(B.class);

        beanDefinitionMap.put("A",aBeanDefinition);
        beanDefinitionMap.put("B",bBeanDefinition);

    }
}
