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
                .compose(r -> {
                    Future<SQLConnection> f1 = Future.future();
                    client.getConnection(f1);
                    return f1;
                })
                .compose(con -> {
                    Future<List<JsonObject>> f2 = Future.future();
                    con.query(sql, r -> {
                        if (r.failed()) {
                            f2.fail(r.cause());
                        }
                        f2.complete(r.result().getRows());
                    });
                    return f2;
                });
    }


    public Future<List<JsonObject>> queryWithParams(AsyncSQLClient client, String sql, JsonArray params) {
        return Future.succeededFuture()
                .compose(r -> {
                    Future<SQLConnection> f1 = Future.future();
                    client.getConnection(f1);
                    return f1;
                })
                .compose(con -> {
                    Future<List<JsonObject>> f2 = Future.future();
                    con.queryWithParams(sql, params, r -> {
                        if (r.failed()) {
                            f2.fail(r.cause());
                        }
                        f2.complete(r.result().getRows());
                    });
                    return f2;
                });
    }

    protected Future<Boolean> updateWithParams(AsyncSQLClient client, String sql, JsonArray params) {
        return Future.succeededFuture()
                .compose(r -> {
                    Future<SQLConnection> f1 = Future.future();
                    client.getConnection(f1);
                    return f1;
                })
                .compose(con -> {
                    Future<Boolean> f2 =  Future.future();
                    con.updateWithParams(sql, params, r -> {
                        if (r.failed()) {
                            f2.fail(r.cause());
                        }
                        f2.complete(r.result().getUpdated() == 1 ? true : false);
                    });
                    return f2;
                });
    }

}