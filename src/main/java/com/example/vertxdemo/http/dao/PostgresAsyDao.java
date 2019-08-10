package com.example.vertxdemo.http.dao;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.AsyncSQLClient;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLConnection;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("singleton")
public class PostgresAsyDao {

    public AsyncSQLClient getClient(Vertx vertx) {
        JsonObject mysqlConfig = new JsonObject().put("host", "127.0.0.1")
                .put("port", 5432)
                .put("maxPollSize", 10)
                .put("username", "postgres")
                .put("password", "123456")
                .put("database", "test");

        return PostgreSQLClient.createShared(vertx, mysqlConfig);
    }

    public Future<SQLConnection> getConnection(AsyncSQLClient client) {
        Future<SQLConnection> future = Future.future();
        client.getConnection(future);
        return future;
    }

    public Future<List<JsonObject>> query(SQLConnection connection, String sql) {
        Future<List<JsonObject>> future = Future.future();
        connection.query(sql, r -> {
            if (r.failed()) {
                future.fail(r.cause());
                future.isComplete();
            }
            future.complete(r.result().getRows());
        });
        return future;
    }

    public Future<List<JsonObject>> queryWithParams(SQLConnection connection, String sql, JsonArray params) {
        Future<List<JsonObject>> future = Future.future();
        connection.queryWithParams(sql, params, r -> {
            if (r.failed()) {
                future.fail(r.cause());
            }
            future.complete(r.result().getRows());
        });
        return future;
    }

    protected Future<Boolean> updateWithParams(SQLConnection connection, String sql, JsonArray params) {
        Future<Boolean> future =  Future.future();
        connection.updateWithParams(sql, params, r -> {
            if (r.failed()) {
                future.fail(r.cause());
            }
            future.complete(r.result().getUpdated() == 1 ? true : false);
        });
        return future;
    }

}