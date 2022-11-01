package com.dexlace.boot.selector;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        System.out.println("MyImportSelector");
        return new String[]{"com.mooc.sb2.ioc.xml.Cat", "com.mooc.sb2.ioc.xml.Dog"};
    }
}
