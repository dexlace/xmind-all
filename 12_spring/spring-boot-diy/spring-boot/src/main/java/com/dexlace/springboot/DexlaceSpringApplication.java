package com.dexlace.springboot;


import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Map;

public class DexlaceSpringApplication {

    public static void run(Class<?> primarySource){
        // 构造一个spring容器
        AnnotationConfigWebApplicationContext webApplicationContext = new AnnotationConfigWebApplicationContext();
        // 配置类传入
        webApplicationContext.register(primarySource);
        webApplicationContext.refresh();


        WebServer webServer = getWebServer(webApplicationContext);

        // 启动tomcat
       webServer.start();

    }

    private static WebServer getWebServer(WebApplicationContext webApplicationContext) {
        Map<String, WebServer> beansOfType = webApplicationContext.getBeansOfType(WebServer.class);
        if (beansOfType.size()==0){
            throw new NullPointerException();
        }
        //  启动tomcat或者jetty
        if (beansOfType.size()>1){
            throw new IllegalStateException();
        }

        return beansOfType.values().stream().findFirst().get();

    }

    private static void startTomcat(WebApplicationContext applicationContext) {
        Tomcat tomcat=new Tomcat();

        Server server=tomcat.getServer();
        Service serverService = server.findService("Tomcat");

        Connector connector = new Connector();
        connector.setPort(8081);

        StandardEngine standardEngine = new StandardEngine();
        standardEngine.setDefaultHost("localhost");

        StandardHost standardHost = new StandardHost();
        standardHost.setName("localhost");

        String contextPath="";
        StandardContext context = new StandardContext();
        context.setPath(contextPath);
        context.addLifecycleListener(new Tomcat.FixContextListener());

        standardHost.addChild(context);
        standardEngine.addChild(standardHost);

        serverService.setContainer(standardEngine);
        serverService.addConnector(connector);

        tomcat.addServlet(contextPath,"dispatcher",new DispatcherServlet(applicationContext));
        context.addServletMappingDecoded("/*","dispatcher");

        try {
            tomcat.start();
        }catch (LifecycleException e){
            e.printStackTrace();
        }



    }
}
