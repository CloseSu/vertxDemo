package com.example.vertxdemo.request2;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpFutureQueue {

    public static final HttpClientOptions options = new HttpClientOptions()
            .setKeepAlive(false);

    private AtomicInteger worker = new AtomicInteger(0);
    private Integer limit;

    private Vertx vertx;

    private ConcurrentLinkedDeque<Future<HttpClient>> pending = new ConcurrentLinkedDeque<>();

    public HttpFutureQueue(Vertx vertx, Integer limit) {
        this.vertx = vertx;
        this.limit = limit;  //期望值 6
    }

    private void execute(Future<HttpClient> task, HttpClient client) {
        vertx.runOnContext(r -> task.tryComplete(client));
    }

    public synchronized void next(HttpClient client) {
        if (!pending.isEmpty())
            execute(pending.pollFirst(), client);
        else {
            client.close();
            worker.decrementAndGet();
        }
    }

    public synchronized void execute(Future<HttpClient> task) {
        if (worker.get() < limit) {
            HttpClient client = vertx.createHttpClient(options);
            worker.incrementAndGet();
            System.out.println("--------------------------set client:  "+ client);
            this.execute(task, client);
        } else{
            System.out.println("pending");
            this.pending.offerFirst(task);
        }
    }

}
