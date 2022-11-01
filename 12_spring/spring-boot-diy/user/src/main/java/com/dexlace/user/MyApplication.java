package com.dexlace.user;


import com.dexlace.springboot.DexlaceSpringApplication;
import com.dexlace.springboot.DexlaceSpringBootApplication;
import com.dexlace.springboot.WebServerAutoConfiguration;
import org.springframework.context.annotation.Import;


@DexlaceSpringBootApplication
//@Import(WebServerAutoConfiguration.class)
public class MyApplication {

//
//    @Bean
//    public TomcatServer tomcatServer(){
//        return new TomcatServer();
//    }

    public static void main(String[] args) {
        DexlaceSpringApplication.run(MyApplication.class);
    }
}
