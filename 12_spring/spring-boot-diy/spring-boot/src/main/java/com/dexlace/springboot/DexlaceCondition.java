package com.dexlace.springboot;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class DexlaceCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {

        Map<String, Object> attributes = annotatedTypeMetadata.getAnnotationAttributes(ConditionalOnClass.class.getName());

        String className =(String)attributes.get("value");


        // 通过类加载器来加载是否存在这个类
        try {
           conditionContext.getClassLoader().loadClass(className);
           return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
