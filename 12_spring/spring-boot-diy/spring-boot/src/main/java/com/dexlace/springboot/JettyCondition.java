//package com.dexlace.springboot;
//
//
//import org.springframework.context.annotation.Condition;
//import org.springframework.context.annotation.ConditionContext;
//import org.springframework.core.type.AnnotatedTypeMetadata;
//
//
//public class JettyCondition implements Condition {
//
//    @Override
//    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
//
//
//        // 通过类加载器来加载是否存在这个类
//        try {
//           conditionContext.getClassLoader().loadClass("org.eclipse.jetty.server.Server");
//           return true;
//        } catch (ClassNotFoundException e) {
//            return false;
//        }
//
//    }
//}
