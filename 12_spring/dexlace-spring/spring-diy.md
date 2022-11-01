# diy-spring

## AbstractAutowireCapableBeanFactory

<img src="/Users/dexlace/private-github-repository/xmind-all/12_spring/dexlace-spring/spring-diy.assets/image-20221021164656493.png" alt="image-20221021164656493" style="zoom:50%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/12_spring/dexlace-spring/spring-diy.assets/image-20221021165831577.png" alt="image-20221021165831577" style="zoom: 50%;" />

```java
   @Test
    public void test_BeanFactory(){
        // 1.初始化 BeanFactory
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        // 2.注册 bean
        BeanDefinition beanDefinition = new BeanDefinition(UserService.class);
        beanFactory.registerBeanDefinition("userService", beanDefinition);

        // 3.第一次获取 bean
        // 去缓存拿   拿不到的话直接去beandefinition map 中去拿
        UserService userService = (UserService) beanFactory.getBean("userService");
        userService.queryUserInfo();

        // 4.第二次获取 bean from Singleton
        UserService userService_singleton = (UserService) beanFactory.getBean("userService");
        userService_singleton.queryUserInfo();
    }
```





