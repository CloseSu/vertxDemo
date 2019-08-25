package com.example.vertxdemo.request;

import io.vertx.core.Future;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class SmallQueue {

    private Integer limit;

    private AtomicInteger worker = new AtomicInteger(0);
    private LinkedList<Future> queue = new LinkedList<>();


    public SmallQueue(Integer limit) {
        this.limit = limit;
    }

    public void next() {
        if (!queue.isEmpty()) {
            execute(queue.pop());
        } else {
            worker.decrementAndGet();
        }
    }

    public void execute(Future<Void> task) {
        if (worker.get() < limit) {
            task.tryComplete();
        } else {
            queue.add(task);
        }
    }


}
