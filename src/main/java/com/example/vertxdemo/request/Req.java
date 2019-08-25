package com.example.vertxdemo.request;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.RequestOptions;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;


@Component
public class Req extends AbstractVerticle {

    private final Integer nTos = 1_000_000;
    private final Integer throttle = 5;

    private SmallQueue queue;

    @Override
    public void start() {
        execFuture("70m");
        execFuture("text.txt");
        execFuture("cloud.jpg");
        execFuture("70m");
        execFuture("text.txt");
        execFuture("cloud.jpg");
        execFuture("70m");
        execFuture("70m");
        execFuture("70m");
        execFuture("70m");

    }


    public Future<String> execFuture(String name) {
        return execInqueue(() -> getFile(name), getQueue(), (future) -> queue.execute(future));
    }

    private SmallQueue getQueue() {
        if (queue == null) {
            queue = new SmallQueue(2);
            return queue;
        } else {
            return queue;
        }
    }


    private <T> Future<T> execInqueue(Supplier<Future<T>> func, SmallQueue queue, Consumer<Future<Void>> queueFunc) {
        Future<T> result = Future.future();
        Future<Void> trigger = Future.future();
        trigger.compose(o -> func.get())
                .setHandler(r -> {
                    if (r.succeeded()) {
                        result.complete(r.result());
                    } else {
                        result.fail(r.cause());
                    }
                    queue.next();
                    System.out.println("run next");
                });
        queueFunc.accept(trigger);
        return result;
    }

    private Future<String> getFile(String name) {
       HttpClientOptions options = new HttpClientOptions()
                .setKeepAlive(false);
        HttpClient client = vertx.createHttpClient(options);

        Future<String> f = Future.future();
        RequestOptions r = new RequestOptions();
        r.setHost("localhost");
        r.setPort(8082);
        r.setURI("/"+name);

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
                f.complete(b.toString());
            });
        });

        return f;
    }

}
