package com.example.vertxdemo.request2;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class HttpWorkHelper {

    private static HttpFutureQueue queue ;
    private static final Integer LIMIT = 6;

    private static HttpFutureQueue getQueue(Vertx vertx) {
        if (queue == null) {
            queue = new HttpFutureQueue(vertx, LIMIT);
            return queue;
        } else {
            return queue;
        }
    }

    private static <T> Future<T> requestInQueue(Function<HttpClient, Future<T>> httpFunc, HttpFutureQueue queue, Consumer<Future<HttpClient>> queueFunc) {
        Future<T> result = Future.future();
        Future<HttpClient> trigger = Future.future();
        ClientWrapper clientWrapper = new ClientWrapper();
        trigger.map(client -> {
            System.out.println("--------------------------init client:  "+client);
            clientWrapper.client = client;
            return clientWrapper;
        }).compose(wrapper -> httpFunc.apply(wrapper.client))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        result.complete(res.result());
                    } else {
                        System.out.println("[HttpBuffer] getBody error!"+ res.cause());
                        result.fail(res.cause());
                    }
                    System.out.println("[HttpBuffer] getBody finish try queue.next()");
                    queue.next(clientWrapper.client);
                });
        queueFunc.accept(trigger);

        return result;
    }

    public static Future<String> execFuture(Vertx vertx, RequestParams rp, HttpMethod method) {
        return requestInQueue((client) -> request(method, rp, client), getQueue(vertx), (future) -> queue.execute(future));
    }


    public static <T> Future<T> request(HttpMethod method,RequestParams rp, HttpClient client) {
        Future<T> f  = Future.future();
        client.connectionHandler(con -> System.out.println("connected"));
        HttpClientRequest httpClientRequest = client.requestAbs(method, rp.getAbsUrl(), r -> {
            System.out.println("get header:--------------------");
            if (rp.isBackBody()) {
                r.bodyHandler(b -> {
                    System.out.println("get body:------------------------");
                    f.complete((T) b.toString(StandardCharsets.UTF_8));
                });
            } else {
                f.complete();
            }
        }).exceptionHandler(e -> {
            System.out.println("fail");
            try {
                throw e;
            } catch (VertxException | IllegalStateException ex) {
                System.out.println("[HttpHelper] {} request error in handler because [{}], retry create http client" + ex.getMessage());
            } catch (Throwable ex) {
                System.out.println("[HttpHelper] {} request error :" + ex);
                f.fail(e);
            }
        });

        if (rp.isSendHeader()) {
            rp.getHeader().forEach((k,v) -> {
                httpClientRequest.putHeader(k, v);
            });
        }

        if (rp.isBackBody()) {
            httpClientRequest.end(rp.getStrBody());
        } else {
            httpClientRequest.end();
        }

        return f;
    }
}
