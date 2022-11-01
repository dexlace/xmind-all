package com.dexlace.springboot;



public class TomcatServer implements WebServer{
    @Override
    public void start() {
        System.out.println("tomcat server start");
    }
}
