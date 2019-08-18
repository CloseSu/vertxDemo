package com.example.vertxdemo.request;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.RequestOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


@Component
public class Req extends AbstractVerticle {

    private final Integer nTos = 1_000_000;
    private final Integer throttle = 5;

    @Override
    public void start() {
        HttpClient client = vertx.createHttpClient();

        Queue<Integer> queue = new LinkedList<>();

        for (int i = 0; i< 20; i++) {
            queue.add(i);
        }

        while (queue.size() > 0) {
            List<Future> list = new ArrayList<>();
            for (int i = 0; i < throttle ; i++) {
                Future ff = Future.succeededFuture()
                        .compose(o -> {
                            Future<Void> f = Future.future();
                            Integer name = ((LinkedList<Integer>) queue).pop();
                            getFile(String.valueOf(name), client, f);
                            return f;
                        });
                list.add(ff);
            }
            CompositeFuture.all(list).map(r -> {
                System.out.println("consume 5==============================");
                return null;
            });

        }
    }




    private void getFile(String name, HttpClient client, Future<Void> f) {
        RequestOptions r = new RequestOptions();
        r.setHost("localhost");
        r.setPort(8082);
        r.setURI("/70m");

        Long start = System.nanoTime();

        System.out.println(name + " start connect  ");
        client.connectionHandler(res -> {
            Long t1 = System.nanoTime();
            System.out.println(name + " connected: t1 time = " + (t1 - start) / nTos + " ");
        });

        client.getNow(r, res -> {
            Long t2 = System.nanoTime();
            System.out.print(name + " t2: time = " + (t2 - start) / nTos + "  ");
            MultiMap headers = res.headers();
            headers.forEach((entry) -> {
                System.out.print(name + " k : " + entry.getKey() + " v :" + entry.getValue() + "  ");
            });
            System.out.println();
            res.bodyHandler(b -> {
                Long t3 = System.nanoTime();
                System.out.print(name + " get body  ");
                System.out.println(name + " t3: time = " + (t3 - start) / nTos + "  ");
                f.complete();
            });
        });

    }

}
