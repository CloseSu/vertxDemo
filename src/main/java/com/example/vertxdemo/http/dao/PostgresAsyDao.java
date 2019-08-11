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

    public Future<List<JsonObject>> query(AsyncSQLClient client, String sql) {
        return Future.succeededFuture()
                .compose(r -> Future.<SQLConnection>future(f -> client.getConnection(f)))
                .compose(con -> Future.future(f -> con.query(sql, r -> {
                    if (r.failed()) {
                        f.fail(r.cause());
                    }
                    f.complete(r.result().getRows());
                })));
    }


    public Future<List<JsonObject>> queryWithParams(AsyncSQLClient client, String sql, JsonArray params) {
        return Future.succeededFuture()
                .compose(r -> Future.<SQLConnection>future(f -> client.getConnection(f)))
                .compose(con -> Future.future(f -> con.queryWithParams(sql, params, r -> {
                    if (r.failed()) {
                        f.fail(r.cause());
                    }
                    f.complete(r.result().getRows());
                })));
    }

    protected Future<Boolean> updateWithParams(AsyncSQLClient client, String sql, JsonArray params) {
        return Future.succeededFuture()
                .compose(r -> Future.<SQLConnection>future(f -> client.getConnection(f)))
                .compose(con -> Future.future(f ->  con.updateWithParams(sql, params, r -> {
                    if (r.failed()) {
                        f.fail(r.cause());
                    }
                    f.complete(r.result().getUpdated() == 1 ? true : false);
                })));
    }

}