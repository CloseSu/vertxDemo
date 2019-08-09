package com.example.vertxdemo;

import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class VertxdemoApplication {

    @Autowired
    SocketEventBusServer socketEventBusServer;

    public static void main(String[] args) {
        SpringApplication.run(VertxdemoApplication.class, args);
    }


    @PostConstruct
    public void depley() {
        Vertx.vertx().deployVerticle(socketEventBusServer);
    }
}
