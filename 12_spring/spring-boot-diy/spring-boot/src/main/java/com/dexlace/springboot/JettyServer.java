package com.dexlace.springboot;

public class JettyServer  implements WebServer{
    @Override
    public void start() {
        System.out.println("jetty server start");
    }
}
