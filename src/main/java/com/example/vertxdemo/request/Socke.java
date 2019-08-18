package com.example.vertxdemo.request;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.RequestOptions;
import org.springframework.stereotype.Component;

@Component
public class Socke extends AbstractVerticle {

    @Override
    public void start() {
        RequestOptions r = new RequestOptions();
        r.setHost("localhost");
        r.setPort(8082);

        HttpClient client = vertx.createHttpClient();

        client.websocket(r, ws -> {
                System.out.println("connected ");
                ws.writeTextMessage("test");
                ws.frameHandler(f -> {
                   System.out.println(" : " +f.isFinal());
                });
        });
    }
}
