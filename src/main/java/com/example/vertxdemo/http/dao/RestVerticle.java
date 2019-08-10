package com.example.vertxdemo.http.dao;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestVerticle extends AbstractVerticle {

    @Autowired
    private PostgresAsyDao postgresAsyDao;

    private final Integer PORT = 8080;

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route("/test").handler(this::getUserById);

        vertx.createHttpServer().requestHandler(router).listen(PORT, server -> {
            if (server.failed()) {
                System.out.println("HttpServer 启动失败, " + server.cause().getMessage());
                return;
            }
            System.out.println("HttpServer 启动成功, 端口 " + PORT);
        });
    }

    private void getUserById(RoutingContext ctx)  {
        AsyncSQLClient client = postgresAsyDao.getClient(vertx);
        String sql = "SELECT * FROM test.test_table ";
        Future.succeededFuture()
                .compose(r -> postgresAsyDao.getConnection(client))
                .compose(con -> postgresAsyDao.query(con, sql))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        System.out.println(res.result().toString());
                        ctx.response().end(res.result().toString());
                    } else {
                        System.out.println("fail");
                        ctx.failed();
                    }
                });
    }
}
