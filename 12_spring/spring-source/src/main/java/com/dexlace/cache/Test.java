package com.dexlace.cache;

import com.dexlace.cache.diy.A;
import com.dexlace.cache.service.AService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) {
        // 初始化
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(AppConfig.class);

        AService aService =(AService) annotationConfigApplicationContext.getBean("AService");

        aService.test();
    }
}
