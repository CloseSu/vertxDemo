package com.example.vertxdemo.request;

import com.example.vertxdemo.util.Asserts;
import com.example.vertxdemo.util.HttpStatus;
import com.example.vertxdemo.vo.AuthorizeVO;
import com.example.vertxdemo.vo.DataVO;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileUploadVerticle extends AbstractVerticle {

    private final String path = "file-upload";
    private static final String URI = "/*";

    public static final String ACCESS_KEY = System.getenv("ACCESS_KEY");
    public static final String SECRET_KEY = System.getenv("SECRET_KEY");

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

//        router.post(URI).handler(this::interceptor);
//        router.get(URI).handler(this::interceptor);
        router.route().handler(BodyHandler.create());

        router.route("/:bucket").handler(this::getFile);
        router.route("/upload/:bucket");
        router.route("/upload/:bucket").handler(this::uploadProcess);
        router.route("/upload/:bucket/:key").handler(this::uploadProcess);
        router.route("/remove/:bucket").handler(this::removeProcess);
        router.route("/list/:bucket").handler(this::listProcess);

        errorProcess(router);

        server.requestHandler(router).listen(8082);

        createDir();
    }

    private void createDir() {
        FileSystem fs = vertx.fileSystem();
        Future.succeededFuture()
                .compose(r -> {
                    Future<Void> f = Future.future();
                    fs.exists(path, result -> {
                        if (result.succeeded() && result.result()) {
                            log.debug("------------- dir exist    ---------------");
                        } else {
                            fs.mkdir(path, f);
                            log.debug("------------- dir not exist ---------------");
                        }
                    });
                    return f;
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[FileUploadVerticle] create dir success", rs.result());
                    }
                });
    }


    private void getFile(RoutingContext ctx) {
        String picture = ctx.request().getParam("bucket");
        Future.succeededFuture().compose(r -> {
            FileSystem fs = vertx.fileSystem();
            Future<Buffer> f = Future.future();
            fs.readFile(path+File.separator+picture, f);
            return f;
        }).setHandler(rs -> {
            if (rs.succeeded()) {
                log.debug("[FileUploadVerticle] getFile success uri: /:bucket, param: [bucket:{}] , return : [File:{}] ", picture, picture);
                ctx.response().putHeader("Access-Control-Allow-Origin", "*").end(rs.result());
            } else {
                ctx.fail(rs.cause());
            }
        });
    }

    private void listProcess(RoutingContext ctx) {
        String offsetFinal = requestParamProcess("offset", ctx, "0");
        String limitFinal = requestParamProcess("limit", ctx, "0");
        FileSystem fs = vertx.fileSystem();
        Future.succeededFuture()
                .compose(r -> {
                    Future<List<String>> outList = Future.future();
                    fs.readDir(path, outList);
                    return outList;
                })
                .map(list -> list.stream()
                        .sorted()
                        .skip(Long.valueOf(offsetFinal))
                        .limit(limitFinal.equals("0") ? list.size() : Long.valueOf(limitFinal))
                        .map(s -> {
                            DataVO data = new DataVO();
                            data.setKey(s.substring(s.lastIndexOf(File.separator)+1));
                            data.setPath(s);
                            return data;
                        })
                        .collect(Collectors.toList())
                )
                .compose(res -> {
                    List<Future> fList = new ArrayList<>();
                    res.forEach(dataVo -> {
                        Future ff = Future.succeededFuture().compose(o -> {
                            Future<Buffer> f = Future.future();
                            fs.readFile(dataVo.getPath(), f);
                            return f;
                        });
                        fList.add(ff);
                    });
                    return CompositeFuture.all(fList).map(rr -> {
                        for (int i = 0; i< fList.size(); i++) {
                            Buffer b  = rr.resultAt(i);
                            res.get(i).setHash(DigestUtils.md5Hex(b.getBytes()));
                        }
                        return res;
                    });
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[FileUploadVerticle] listProcess success uri: /list/:bucket, param: [offset:{}, limit:{}], return : {} ", offsetFinal, limitFinal, rs.result());
                        ctx.response().end(new Gson().toJson(rs.result()));
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }


    private void removeProcess(RoutingContext ctx) {
        List<String> paramList = new Gson().fromJson(ctx.getBodyAsString(), new TypeToken<List<String>>() {}.getType());
        Future.succeededFuture()
                .map(r -> {
                    FileSystem fs = vertx.fileSystem();
                    paramList.forEach(filename -> {
                        Future<Void> f = Future.future();
                        String filepath = path + File.separator + filename;
                        fs.delete(filepath, f);
                    });
                    return r;
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[FileUploadVerticle] removeProcess success uri: /remove/:bucket, param: {}, return : none ", paramList.toString());
                        ctx.response().end();
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }


    private void uploadProcess(RoutingContext ctx) {
        String name = requestParamProcess("key", ctx, UUID.randomUUID().toString());
        FileSystem fs = vertx.fileSystem();
        Future.succeededFuture()
                .map(r -> {
                    Future<Void> f = Future.future();
                    fs.writeFile(path+File.separator+ name, ctx.getBody(), f);
                    return f;
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[FileUploadVerticle] uploadProcess success " +
                                  "uri: /upload/:bucket & /upload/:bucket/:key, " +
                                  "param: [key:{}], return : none ", name);
                        ctx.response().end();
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }

    private void interceptor(RoutingContext ctx) {
        Future.succeededFuture().map(v -> {
            if (ctx.request().uri().split("/").length <= 2) {
                return Future.succeededFuture(null);
            }
            String finalToken = ctx.request().getHeader(HttpHeaders.AUTHORIZATION);

            Asserts.assertNotNull(finalToken, RuntimeException::new);
            AuthorizeVO authorize = new Gson().fromJson(new String(Base64Utils.decodeFromString(finalToken)), AuthorizeVO.class);

            log.debug(", token: {}, accesskey: {}, secretKey: {}", finalToken, authorize.accessKey, authorize.secretKey);

            Asserts.assertEquals(authorize.accessKey, ACCESS_KEY, RuntimeException::new);
            Asserts.assertEquals(authorize.secretKey, SECRET_KEY, RuntimeException::new);
            return v;
        }).setHandler(r -> {
            if (r.succeeded()) {
                log.debug("token validate success ");
                ctx.response().putHeader("Access-Control-Allow-Origin", "*");
                ctx.next();
            } else {
                log.debug("token validate failed ");
                ctx.fail(r.cause());
            }
        });
    }

    private void errorProcess( Router router) {
        router.route().failureHandler(ctx -> {
            log.error("[FileUploadVerticle] Failure :", ctx.failure());
            try {
                throw ctx.failure();
            } catch (FileSystemException e) {
                ctx.response().setStatusCode(HttpStatus.METHOD_NOT_ALLOWED).end();
            } catch (RuntimeException e) {
                ctx.response().setStatusCode(HttpStatus.BAD_REQUEST).end();
            }  catch (Throwable t) {
                ctx.response().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR).end();
            }
        });
    }

    private String requestParamProcess(String target, RoutingContext ctx, String defaultValue) {
        String s = ctx.request().getParam(target);
        return Strings.isNullOrEmpty(s) ? defaultValue : s;
    }
}
