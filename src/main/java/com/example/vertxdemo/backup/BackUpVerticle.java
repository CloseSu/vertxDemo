package com.example.vertxdemo.backup;

import com.example.vertxdemo.backup.vo.BackupFileVo;
import com.example.vertxdemo.backup.vo.BackupInfoVo;
import com.example.vertxdemo.backup.vo.ReturnBackupInfoVo;
import com.example.vertxdemo.backup.vo.ReturnListVo;
import com.example.vertxdemo.util.Asserts;
import com.example.vertxdemo.util.HttpStatus;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Comparator.reverseOrder;

@Slf4j
@Component
public class BackUpVerticle extends AbstractVerticle {
    private final String ACTIONBACKUP = "backup";
    private final String ACTIONRESTORE = "restore";
    private final String ACTIONTAR = "tar";

    private static ConcurrentHashMap<String, String> backdirMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Long> pidMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Long, BackupInfoVo> retoreMap = new ConcurrentHashMap<>();

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route("/list").handler(this::list);
        router.route("/backup").handler(this::backup);
        router.route("/restore").handler(this::restore);
        router.route("/file").handler(this::getFile);
        router.route("/status").handler(this::process);
        router.route("/remove").handler(this::remove);
        errorProcess(router);
        server.requestHandler(router).listen(8080);
        scanPidProcess();
    }

    private void remove(RoutingContext ctx) {
        BackupInfoVo backupInfoVo = new Gson().fromJson(ctx.getBodyAsString(), BackupInfoVo.class);
        Asserts.assertNotNull(backupInfoVo.getRemovedays(), RuntimeException::new);
        FileSystem fs = vertx.fileSystem();
        Future.succeededFuture()
                .compose(o -> {
                    Future<List<String>> list = Future.future();
                    fs.readDir(backupInfoVo.getPath(), list);
                    return list;
                }).map(list -> list.stream()
                .map(s -> {
                    BackupFileVo data = new BackupFileVo();
                    data.setPath(s);
                    data.setFilename(backupInfoVo.getPath() + s.split(backupInfoVo.getPath())[1]);
                    data.setTime(s.split("_")[3]);
                    return data;
                })
                .filter(s -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                    LocalDateTime then = LocalDateTime.now().minusDays(backupInfoVo.getRemovedays());
                    LocalDateTime dateTime = LocalDateTime.parse(s.getTime(), formatter);
                    return dateTime.compareTo(then) < 1;
                })
                .sorted(Comparator.comparing(BackupFileVo::getTime))
                .collect(Collectors.toList())
        )
                .compose(list -> {
                    List<Future> fList = new ArrayList<>();
                    list.forEach(dataVo -> {
                        Future ff = Future.succeededFuture().compose(o -> {
                            Future<Void> f = Future.future();
                            fs.deleteRecursive(dataVo.getPath(), true, f);
                            return f;
                        });
                        fList.add(ff);
                    });
                    return CompositeFuture.all(fList);
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        ctx.response().putHeader("Access-Control-Allow-Origin", "*").end();
                        log.debug("[BackupVerticle] remove done:  ");
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }

    private void process(RoutingContext ctx) {
        BackupInfoVo backupInfoVo = new Gson().fromJson(ctx.getBodyAsString(), BackupInfoVo.class);
        ReturnBackupInfoVo returnBackupInfoVo = new ReturnBackupInfoVo();
        returnBackupInfoVo.setPid(backupInfoVo.getPid());
        Future.succeededFuture()
                .map(o -> isProcessIdRunning(returnBackupInfoVo))
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[BackupVerticle] process is running ? : {}", returnBackupInfoVo.getOnprocess());
                        ctx.response().putHeader("Access-Control-Allow-Origin", "*").end(new Gson().toJson(returnBackupInfoVo));
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }

    private void list(RoutingContext ctx) {
        BackupInfoVo backupInfoVo = new Gson().fromJson(ctx.getBodyAsString(), BackupInfoVo.class);
        Asserts.assertStringNullOrEmpty(backupInfoVo.getPath(), RuntimeException::new);
        Asserts.assertStringNullOrEmpty(backupInfoVo.getType(), RuntimeException::new);
        ReturnListVo returnListVo = new ReturnListVo();
        FileSystem fs = vertx.fileSystem();
        Future.succeededFuture()
                .compose(o -> {
                    Future<List<String>> list = Future.future();
                    fs.readDir(backupInfoVo.getPath(), list);
                    return list;
                })
                .map(list -> list.stream()
                        .map(s -> {
                            BackupFileVo data = new BackupFileVo();
                            data.setFilename(backupInfoVo.getPath() + s.split(backupInfoVo.getPath())[1]);
                            if (backupInfoVo.getType().equals("mongodb")
                                    && !data.getFilename().matches(".*tar.gz")
                                    && data.getFilename().matches(".*mongodb.*")
                            ) {
                                data.setFilename(data.getFilename()+".tar.gz");
                            }
                            data.setTime(s.split("_")[3]);
                            return data;
                        })
                        .filter(s -> {
                            if (backupInfoVo.getType().equals("mongodb")) {
                                return s.getFilename().matches(".*tar.gz");
                            } else {
                                if (Strings.isNullOrEmpty(backupInfoVo.getFilename())) {
                                    return true;
                                } else {
                                    String pattern =".*"+backupInfoVo.getFilename()+".*";
                                    return s.getFilename().matches(pattern);
                                }
                            }
                        })
                        .sorted(Comparator.comparing(BackupFileVo::getTime, reverseOrder()))
                        .collect(Collectors.toList())
                )
                .map(list -> {
                    List<String> returnList = new ArrayList<>();
                    Integer total = list.size();
                    list.forEach(backVo -> {returnList.add(backVo.getFilename());});
                    if(backupInfoVo.getPagesize() != null && backupInfoVo.getPagesize() > 0 &&
                            backupInfoVo.getPageindex() != null && backupInfoVo.getPageindex() > 0 &&
                            total > backupInfoVo.getPagesize()
                    ) {
                        Integer pageSize = backupInfoVo.getPagesize();
                        Integer pageIndex = backupInfoVo.getPageindex();
                        Integer pageIndexRaw = total / pageSize;
                        Integer pageIndexRemain = total % backupInfoVo.getPagesize();
                        Integer start = (pageIndex - 1) * pageSize;
                        Integer end = (pageIndex - 1) < pageIndexRaw? pageIndex * pageSize : start + pageIndexRemain;

                        List<String> subList = returnList.subList(start, end);
                        returnListVo.setCount(total);
                        returnListVo.setList(subList);
                        return returnListVo;
                    } else {
                        returnListVo.setCount(total);
                        returnListVo.setList(returnList);
                        return returnListVo;
                    }
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        ctx.response().putHeader("Access-Control-Allow-Origin", "*").end(new Gson().toJson(rs.result()));
                        log.debug("[BackupVerticle] list data: {} ", rs.result().toString());
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }

    private void backup(RoutingContext ctx) {
        BackupInfoVo backupInfoVo = new Gson().fromJson(ctx.getBodyAsString(), BackupInfoVo.class);
        ReturnBackupInfoVo returnBackupInfoVo = new ReturnBackupInfoVo();
        FileSystem fs = vertx.fileSystem();
        Future.succeededFuture()
                .compose(r -> createDir(backupInfoVo.getPath()))
                .compose(o -> {
                    backdirMap.put(backupInfoVo.getPath(), backupInfoVo.getPath());
                    String formatDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    String fullname = backupInfoVo.getBackupname() + "_" + backupInfoVo.getType() + "_" + backupInfoVo.getDatabase()+"_"+formatDateTime;
                    returnBackupInfoVo.setBackuname(fullname);
                    backupInfoVo.setBackupname(fullname);
                    String command = getCommand(backupInfoVo, ACTIONBACKUP);
                    return runCommand(backupInfoVo,command, ACTIONBACKUP);
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[BackupVerticle] backup success data: {}", returnBackupInfoVo);
                        ctx.response().putHeader("Access-Control-Allow-Origin", "*").end(new Gson().toJson(returnBackupInfoVo));
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }

    private void restore(RoutingContext ctx) {
        BackupInfoVo backupInfoVo = new Gson().fromJson(ctx.getBodyAsString(), BackupInfoVo.class);
        Future.succeededFuture()
                .compose(o -> {
                    Future<Long> longFuture = Future.future();
                    if (backupInfoVo.getType().equals("mongodb")){
                        String command = "tar -xvf " + backupInfoVo.getBackupname();
                        return runCommand(backupInfoVo,command, ACTIONRESTORE);
                    } else {
                        longFuture.complete();
                        return longFuture;
                    }
                })
                .map(pid -> {
                    if (pid != null) {
                        if (backupInfoVo.getType().equals("mongodb")) {
                            String backUpPath = backupInfoVo.getPath();
                            backUpPath += File.separator + backupInfoVo.getBackupname().replace(".tar.gz", "");
                            backupInfoVo.setPath(backUpPath);
                            backupInfoVo.setAction(ACTIONTAR);
                            retoreMap.put(pid, backupInfoVo);
                        }
                    }
                    return null;
                })
                .compose(o -> {
                    Future<Long> longFuture = Future.future();
                    if (!backupInfoVo.getType().equals("mongodb")) {
                        String command = getCommand(backupInfoVo, ACTIONRESTORE);
                        return runCommand(backupInfoVo,command, ACTIONRESTORE);
                    } else {
                        longFuture.complete();
                        return longFuture;
                    }
                })
                .setHandler(rs -> {
                    if (rs.succeeded()) {
                        log.debug("[BackupVerticle] restore success");
                        String res = "";
                        ctx.response().putHeader("Access-Control-Allow-Origin", "*").end();
                    } else {
                        ctx.fail(rs.cause());
                    }
                });
    }


    private void getFile(RoutingContext ctx) {
        FileSystem fs = vertx.fileSystem();
        BackupInfoVo backupInfoVo = new Gson().fromJson(ctx.getBodyAsString(), BackupInfoVo.class);
        Future.succeededFuture().compose(r -> {
            Future<Buffer> f = Future.future();
            fs.readFile(backupInfoVo.getPath(), f);
            return f;
        }).setHandler(rs -> {
            if (rs.succeeded()) {
                log.debug("[BackUpVerticle] getFile {} success , return binary ", fs);
                ctx.response().putHeader("Access-Control-Allow-Origin", "*").end(rs.result());
            } else {
                ctx.fail(rs.cause());
            }
        });
    }

    private Future<Void> createDir(String path) {
        FileSystem fs = vertx.fileSystem();
        Future<Void> f = Future.future();
        fs.exists(path, result -> {
            if (result.succeeded() && result.result()) {
                log.debug("------------- dir exist    ---------------");
                f.complete();
            } else {
                log.debug("------------- dir not exist,  will create one---------------");
                fs.mkdir(path, f);
            }
        });
        return f;
    }

    private String getCommand(BackupInfoVo backupInfoVo, String action) {
        String command = null;

        Asserts.assertStringNullOrEmpty(backupInfoVo.getHost(), RuntimeException::new);
        Asserts.assertStringNullOrEmpty(backupInfoVo.getPort(), RuntimeException::new);
        Asserts.assertStringNullOrEmpty(backupInfoVo.getType(), RuntimeException::new);
        Asserts.assertStringNullOrEmpty(backupInfoVo.getBackupname(), RuntimeException::new);
        Asserts.assertStringNullOrEmpty(backupInfoVo.getPath(), RuntimeException::new);

        switch (backupInfoVo.getType()) {
            case "postgresql":
                Asserts.assertStringNullOrEmpty(backupInfoVo.getDatabase(), RuntimeException::new);
                Asserts.assertStringNullOrEmpty(backupInfoVo.getDbuser(), RuntimeException::new);
                command =  getPostgreCommand(backupInfoVo, action);
                break;
            case "mongodb":
                Asserts.assertStringNullOrEmpty(backupInfoVo.getDatabase(), RuntimeException::new);
                command =  getMongoCommand(backupInfoVo, action);
                break;
            case "redis":
                command =  getRedisCommand(backupInfoVo, action);
                break;
        }
        return command;
    }

    private String getRedisCommand(BackupInfoVo backupInfoVo, String action) {
        StringBuffer sb = new StringBuffer();
        sb.append(action.equals(ACTIONBACKUP)? "" : "rdb --c protocol "+ backupInfoVo.getBackupname() + " | ");
        sb.append(" redis-cli ");
        sb.append(" -h ").append(backupInfoVo.getHost());
        sb.append(" -p ").append(backupInfoVo.getPort());
        sb.append(action.equals(ACTIONBACKUP)? " --rdb " + backupInfoVo.getBackupname() : " --pipe ");

        return sb.toString();
    }

    private String getMongoCommand(BackupInfoVo backupInfoVo, String action) {
        StringBuffer sb = new StringBuffer();
        sb.append(action.equals(ACTIONBACKUP)? " mongodump " : " mongorestore ");
        sb.append(" -h ").append(backupInfoVo.getHost()+":"+backupInfoVo.getPort());
        sb.append(Strings.isNullOrEmpty(backupInfoVo.getDbuser())? "" : " -u " + backupInfoVo.getDbuser());
        sb.append(Strings.isNullOrEmpty(backupInfoVo.getPassword())? "" : "-p " + backupInfoVo.getPassword());
        sb.append(" -d ").append(backupInfoVo.getDatabase());
        sb.append(action.equals(ACTIONBACKUP)? " -o "+ backupInfoVo.getBackupname(): " "+ backupInfoVo.getDatabase());
        return sb.toString();
    }

    private String getPostgreCommand(BackupInfoVo backupInfoVo, String action) {
        StringBuffer sb = new StringBuffer();
        sb.append(Strings.isNullOrEmpty(backupInfoVo.getPassword())? "": "PGPASSWORD=\""+ backupInfoVo.getPassword()+"\"");
        sb.append(action.equals(ACTIONBACKUP)? " pg_dump " : " psql ");
        sb.append(" -h ").append(backupInfoVo.getHost());
        sb.append(" -p ").append(backupInfoVo.getPort());
        if (!Strings.isNullOrEmpty(backupInfoVo.getDbuser())) {
            sb.append(" -U ").append(backupInfoVo.getDbuser());
        }
        sb.append(" -f ").append(action.equals(ACTIONBACKUP)? backupInfoVo.getBackupname() : backupInfoVo.getBackupname());
        sb.append(" -d ").append(backupInfoVo.getDatabase());
        return sb.toString();
    }

    private Future<Long> runCommand(BackupInfoVo backupInfoVo, String command, String action) {
        Future<Long> f = Future.future();
        String[] cmd = {"/bin/sh", "-c", "cd " + backupInfoVo.getPath() + " ; nohup "+command+" ; "};

        log.debug("[BackupVerticle] runCommand path: {} command : {}", backupInfoVo.getPath(), command);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (Strings.isNullOrEmpty(command)) {
            f.fail(new NullPointerException("no command string"));
            return f;
        }
        try {
            Process p = pb.redirectErrorStream(true).start();
            if (action.equals(ACTIONBACKUP)){
                pidMap.put(backupInfoVo.getBackupname(),p.pid());
            }
            f.complete(p.pid());
        } catch (IOException e) {
            f.fail(e);
        }
        return f;
    }

    private ReturnBackupInfoVo isProcessIdRunning(ReturnBackupInfoVo vo) {
        String command = "ps -p " + vo.getPid();
        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);
            InputStreamReader isReader = new InputStreamReader(pr.getInputStream());
            BufferedReader bReader = new BufferedReader(isReader);
            String strLine;
            while ((strLine= bReader.readLine()) != null) {
                if (strLine.contains(" " + vo.getPid() + " ")) {
                    vo.setOnprocess(true);
                    return vo;
                }
            }
            String formatDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            vo.setEndtime(formatDateTime);
            vo.setOnprocess(false);
            return vo;
        } catch (Exception ex) {
            vo.setOnprocess(true);
            return vo;
        }
    }

    private Future<Integer> getSizeByCommand(String target) {
        String[] cmd = {"/bin/sh", "-c", " du " + target };
        Future<Integer> f = Future.future();
        ProcessBuilder pb = new ProcessBuilder(cmd);
        try {
            Process p = pb.redirectErrorStream(true).start();
            InputStreamReader isReader = new InputStreamReader(p.getInputStream());
            BufferedReader bReader = new BufferedReader(isReader);
            String strLine;
            while ((strLine= bReader.readLine()) != null) {
                log.debug("get size key word: {}", strLine.split("\t")[0].strip());
                String size = strLine.split("\t")[0].strip();
                f.complete(Integer.parseInt(size));
                break;
            }
        } catch (IOException e) {
            f.fail(e.getCause());
        }
        return f;
    }


    private void scanPidProcess() {
        FileSystem fs = vertx.fileSystem();
        vertx.setPeriodic(1000, id -> {
            backdirMap.forEach((dir, val) -> {
                Future.succeededFuture()
                        .compose(o -> {
                            Future<List<String>> f = Future.future();
                            fs.readDir(dir, f);
                            return f;
                        })
                        .map(list -> {
                            List<ReturnBackupInfoVo> voList = new ArrayList<>();
                            list.forEach(filePath -> {
                                String fileName = filePath.split(dir)[1].replace(File.separator, "");
                                Long pid = pidMap.get(fileName);
                                if (pid != null) {
                                    log.debug("[BackupVerticle] routine process pid: {} ", pid);
                                    ReturnBackupInfoVo vo = new ReturnBackupInfoVo();
                                    vo.setFullpath(filePath);
                                    vo.setBackuppath(dir);
                                    vo.setBackuname(fileName);
                                    vo.setType(fileName.split("_")[1]);
                                    vo.setPid(pid);
                                    vo.setDatabase(fileName.split("_")[2]);
                                    vo = isProcessIdRunning(vo);
                                    if (!vo.getOnprocess()) { //means task done
                                        log.debug("[BackupVerticle] routine process end pid: {} ", pid);
                                        voList.add(vo);
                                        pidMap.remove(fileName);
                                    }
                                }
                            });
                            return voList;
                        })
                        .compose(voList -> {
                            List<Future> fList = new ArrayList<>();
                            voList.forEach(vo -> {
                                fList.add(getSizeByCommand(vo.getFullpath()));
                            });

                            return CompositeFuture.all(fList).map(rr -> {
                                for (int i = 0; i < fList.size(); i++) {
                                    Integer size= rr.resultAt(i);
                                    voList.get(i).setSize(size);
                                }
                                return voList;
                            });
                        })
                        .compose(volist -> {
                            List<Future> fList = new ArrayList<>();
                            volist.forEach(vo -> {
                                Future<Void> f = Future.future();
                                String from = vo.getBackuppath() + File.separator + vo.getBackuname();
                                String to = from+ "_" + vo.getEndtime() + "_" + vo.getSize();
                                fs.move(from, to, f);
                                fList.add(f);
                            });
                            return CompositeFuture.all(fList);
                        })
                        .compose(o -> {
                            Future<List<String>> list = Future.future();
                            fs.readDir(dir, list);
                            return list;
                        })
                        .map(list -> list.stream()
                                .filter(s -> s.matches(".*mongodb.*"))
                                .filter(s -> !s.matches(".*tar.gz"))
                                .collect(Collectors.toList())
                        )
                        .compose(list -> {
                            List<Future> fList = new ArrayList<>();
                            list.forEach(mongoDir -> {
                                if (retoreMap.isEmpty()) {
                                    if (mongoDir.split("_").length > 4) {
                                        BackupInfoVo backupInfoVo = new BackupInfoVo();
                                        backupInfoVo.setPath(dir);
                                        String splitPath = mongoDir.split(backupInfoVo.getPath())[1].replace(File.separator, "");
                                        String command = "tar -czvf " + splitPath + ".tar.gz " + splitPath + File.separator;
                                        fList.add(runCommand(backupInfoVo, command, ""));
                                    }
                                }
                            });
                            return CompositeFuture.all(fList).map(r -> list);
                        })
                        .compose(list -> {
                            List<Future> fList = new ArrayList<>();
                            if (!retoreMap.isEmpty()){
                                retoreMap.forEach((pid,backupInfoVo) -> {
                                    ReturnBackupInfoVo vo = new ReturnBackupInfoVo();
                                    vo.setBackuppath(dir);
                                    vo.setPid(pid);
                                    vo = isProcessIdRunning(vo);
                                    log.debug("[BackupVerticle] routine restore/tar process pid: {} ", pid);
                                    if (!vo.getOnprocess()) { //means task done
                                        log.debug("[BackupVerticle] routine restore/tar process end pid: {} ", pid);
                                        retoreMap.remove(pid);
                                        if (backupInfoVo.getAction().equals(ACTIONTAR)) {
                                            String command = getCommand(backupInfoVo, ACTIONRESTORE);
                                            fList.add(runCommand(backupInfoVo,command, ACTIONRESTORE));
                                        }
                                    }
                                });
                            }
                            return CompositeFuture.all(fList).map(r -> {
                                for (int i = 0; i< fList.size(); i++) {
                                    Long restorePid = r.resultAt(i);
                                    BackupInfoVo tarRestore = new BackupInfoVo();
                                    tarRestore.setAction(ACTIONRESTORE);
                                    tarRestore.setPid(restorePid);
                                    retoreMap.put(restorePid, tarRestore);
                                }
                                return list;});
                        })
                        .compose(list -> {
                            List<Future> fList = new ArrayList<>();
                            list.forEach(mongoDir -> {
                                if (retoreMap.isEmpty()) {
                                    if (mongoDir.split("_").length > 4 && !mongoDir.matches(".*tar.gz")) {
                                        Future ff = Future.succeededFuture().compose(o -> {
                                            Future<Void> f = Future.future();
                                            fs.deleteRecursive(mongoDir, true, f);
                                            return f;
                                        });
                                        fList.add(ff);
                                    }
                                }
                            });
                            return CompositeFuture.all(fList);
                        })
                        .setHandler(rs -> {
                            if (rs.succeeded()) {
                            }
                        });
            });
        });
    }

    private void errorProcess( Router router) {
        router.route().failureHandler(ctx -> {
            log.error("[BackupVerticle] Failure :", ctx.failure());
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
}
