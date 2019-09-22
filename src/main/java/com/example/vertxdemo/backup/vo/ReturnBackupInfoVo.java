package com.example.vertxdemo.backup.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ReturnBackupInfoVo {

    @SerializedName("backuname")
    private String backuname;
    @SerializedName("pid")
    private Long pid;
    @SerializedName("startime")
    private String startime;
    @SerializedName("endtime")
    private String endtime;
    @SerializedName("onprocess")
    private Boolean onprocess;
    @SerializedName("size")
    private Integer size;
    @SerializedName("backuppath")
    private String backuppath;
    @SerializedName("type")
    private String type;
    @SerializedName("database")
    public String database;
    @SerializedName("fullpath")
    public String fullpath;

}
