package com.dexlace.boot.ioc.ann;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
public class Worker {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
