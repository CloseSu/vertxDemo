package com.example.vertxdemo.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

public class ClientVerticle extends AbstractVerticle {

    private final String busAdress = "vertx.cluster.replyHello";

    @Override
    public void start() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("input","向你打招呼...");
        vertx.setPeriodic(2 * 1000, callBack-> vertx.eventBus().<JsonObject>send(busAdress, jsonObject,new DeliveryOptions(), resultBody -> {
            if (resultBody.failed()) {
                System.out.println("Fail for the process!");
            }
            System.out.println("服务端响应结果："+ resultBody.result().body().encode());
        }));
    }
}
