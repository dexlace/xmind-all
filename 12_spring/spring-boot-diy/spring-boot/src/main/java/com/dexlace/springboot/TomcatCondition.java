//package com.dexlace.springboot;
//
//
//import org.springframework.context.annotation.Condition;
//import org.springframework.context.annotation.ConditionContext;
//import org.springframework.core.type.AnnotatedTypeMetadata;
//
//
//
//// 现在由DexlaceCondition替换
//public class TomcatCondition implements Condition {
//
//    @Override
//    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
//        // 有tomcat依赖则返回true
//
//        // 通过类加载器来加载是否存在这个类
//        try {
//           conditionContext.getClassLoader().loadClass("org.apache.catalina.startup.Tomcat");
//           return true;
//        } catch (ClassNotFoundException e) {
//            return false;
//        }
//
//    }
//}
