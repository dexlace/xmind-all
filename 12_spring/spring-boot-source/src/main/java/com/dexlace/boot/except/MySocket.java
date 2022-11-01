package com.dexlace.boot.except;

import java.net.ServerSocket;

public class MySocket {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080);
        serverSocket.accept();
    }

}
