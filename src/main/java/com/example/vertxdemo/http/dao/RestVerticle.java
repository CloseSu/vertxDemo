package com.example.vertxdemo.http.dao;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RestVerticle extends AbstractVerticle {

    @Autowired
    private PostgresAsyDao postgresAsyDao;

    private final Integer PORT = 8080;

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());
        router.route("/query").handler(this::query);
        router.route("/queryWithParams").handler(this::queryWithParams);
        router.route("/insertWithParams").handler(this::insertWithParams);
        router.route("/updateWithParams").handler(this::updateWithParams);
        router.route("/deleteWithParams").handler(this::deleteWithParams);

        vertx.createHttpServer().requestHandler(router).listen(PORT);
    }

    private void query(RoutingContext ctx)  {
        AsyncSQLClient client = postgresAsyDao.getClient(vertx);
        String sql = "SELECT * FROM test.test_table ";
        Future.succeededFuture()
                .compose(r -> postgresAsyDao.getConnection(client))
                .compose(con -> postgresAsyDao.query(con, sql))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        ctx.response().end(res.result().toString());
                    } else {
                        ctx.fail(res.cause());
                    }
                });
    }

    private void queryWithParams(RoutingContext ctx)  {
        String id = ctx.request().getParam("id");
        String sql = "SELECT * FROM test.test_table where id = ? ";
        JsonArray j = new JsonArray();
        j.add(id);

        AsyncSQLClient client = postgresAsyDao.getClient(vertx);
        Future.succeededFuture()
                .compose(r -> postgresAsyDao.getConnection(client))
                .compose(con -> postgresAsyDao.queryWithParams(con, sql, j))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        ctx.response().end(res.result().toString());
                    } else {
                        ctx.fail(res.cause());
                    }
                });
    }


    private void insertWithParams(RoutingContext ctx) {
        String id = ctx.request().getParam("id");
        String name = ctx.request().getParam("name");
        String value = ctx.request().getParam("value");

        String sql = "INSERT INTO test.test_table( id, name, value)  VALUES (?, ?, ?) ";
        JsonArray j = new JsonArray();
        j.add(id);
        j.add(name);
        j.add(value);

        AsyncSQLClient client = postgresAsyDao.getClient(vertx);

        Future.succeededFuture()
                .compose(r -> postgresAsyDao.getConnection(client))
                .compose(con -> postgresAsyDao.updateWithParams(con, sql, j))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        ctx.response().end(res.result().toString());
                    } else {
                        ctx.fail(res.cause());
                    }
                });
    }


    private void updateWithParams(RoutingContext ctx) {
        String id = ctx.request().getParam("id");
        String name = ctx.request().getParam("name");
        String value = ctx.request().getParam("value");

        String sql = "UPDATE test.test_table set name= ?, value=?  WHERE id = ?";
        JsonArray j = new JsonArray();
        j.add(name);
        j.add(value);
        j.add(id);

        AsyncSQLClient client = postgresAsyDao.getClient(vertx);

        Future.succeededFuture()
                .compose(r -> postgresAsyDao.getConnection(client))
                .compose(con -> postgresAsyDao.updateWithParams(con, sql, j))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        ctx.response().end(res.result().toString());
                    } else {
                        ctx.fail(res.cause());
                    }
                });
    }



    private void deleteWithParams(RoutingContext ctx) {
        String id = ctx.request().getParam("id");
        String sql = "delete from test.test_table where id = ?";
        JsonArray j = new JsonArray();
        j.add(id);

        AsyncSQLClient client = postgresAsyDao.getClient(vertx);
        Future.succeededFuture()
                .compose(r -> postgresAsyDao.getConnection(client))
                .compose(con -> postgresAsyDao.updateWithParams(con, sql, j))
                .setHandler(res -> {
                    if (res.succeeded()) {
                        ctx.response().end(res.result().toString());
                    } else {
                        ctx.fail(res.cause());
                    }
                });
    }

}
