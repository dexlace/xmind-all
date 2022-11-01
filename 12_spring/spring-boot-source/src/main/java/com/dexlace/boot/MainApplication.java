package com.dexlace.boot;

import com.dexlace.boot.initializer.SecondInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MainApplication {

	public static void main(String[] args) {
//		SpringApplication springApplication=new SpringApplication(MainApplication.class);
//		springApplication.addInitializers(new SecondInitializer());
//		springApplication.run(args);
		SpringApplication.run(MainApplication.class, args);
	}

}
