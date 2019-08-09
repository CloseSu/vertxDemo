package com.example.vertxdemo.cluster;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;

/**
 * Created by miaoch on 2016/1/6.
 */
public class ServerMain {

    public static void main(String[] args) {
        ClusterManager mgr = new ZookeeperClusterManager();
        VertxOptions options = new VertxOptions().setClusterManager(mgr).setClusterHost("127.0.0.1");
        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                System.out.println("-------------------start deploy clustered event bus------");
                res.result().deployVerticle(new ServerVerticle(),new DeploymentOptions());
            } else {
                System.out.println("Failed: " + res.cause());
            }
        });

    }
}
