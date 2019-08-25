package com.example.vertxdemo.request2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class ReqBasic extends AbstractVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ReqBasic());
    }

    @Override
    public void start() {
//        HttpClientOptions options = new HttpClientOptions();
//        options.setKeepAlive(false);
//        options.setIdleTimeout(5);
//
//        HttpClient client = vertx.createHttpClient(options);
        Map<String, String> map = new HashMap<>();
        map.put("testhead", "this is head");
        RequestParams rp = new RequestParams();
        rp.setAbsUrl("http://localhost:8082/70m");
        rp.setSendBody(true);
        rp.setStrBody("testBody");
        rp.setBackBody(true);
        rp.setHeader(map);
        rp.setSendHeader(true);

        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);
        HttpWorkHelper.execFuture(vertx, rp, HttpMethod.POST);

    }
}
