package com.example.vertxdemo.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
public class ServerVerticle extends AbstractVerticle {
    private String busAddress;

    public ServerVerticle(String eventBusAddress) {
        this.busAddress=eventBusAddress;
    }

    private Handler<Message<JsonObject>> msgHandler() {
        return msg -> {
            JsonObject job = msg.body();
            try {
                System.out.println("接收客户端消息：" + job.encode());
                JsonObject result=new JsonObject();
                result.put("code",200);
                result.put("msg","ok");
                result.put("remark","[回复]客户端你好!");
                msg.reply(result);
            } catch (Exception e) {
                JsonObject error=new JsonObject();
                error.put("code",500);
                error.put("msg","操作失败！");
                error.put("ob",e.getMessage());
                msg.reply(error);
            }
        };

    }
    @Override
    public void start() {
        vertx.eventBus().<JsonObject>consumer(busAddress).handler(msgHandler());
    }
}
