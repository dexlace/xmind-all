package com.dexlace.boot.pro;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ResultCommandLineRunner implements CommandLineRunner, EnvironmentAware, MyAware {

    private Environment env;

    private Flag flag;

    @Override
    public void run(String... args) throws Exception {
        System.out.println(env.getProperty("mooc.defalut.name"));
        System.out.println(env.getProperty("mooc.active.name"));
    }

    @Override
    public void setEnvironment(Environment environment) {
        env = environment;
    }

    @Override
    public void setFlag(Flag fla) {
        flag = fla;
    }
}
