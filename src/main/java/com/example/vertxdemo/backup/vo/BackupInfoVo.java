package com.example.vertxdemo.backup.vo;


import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class BackupInfoVo {
    @SerializedName("host")
    public String host;
    @SerializedName("port")
    public String port;
    @SerializedName("type")
    public String type;
    @SerializedName("dbuser")
    public String dbuser;
    @SerializedName("password")
    public String password;
    @SerializedName("database")
    public String database;
    @SerializedName("backupname")
    public String backupname;
    @SerializedName("path")
    public String path;
    @SerializedName("backuppath")
    private String backuppath;
    @SerializedName("pagesize")
    public Integer pagesize;
    @SerializedName("pageindex")
    public Integer pageindex;
    @SerializedName("filename")
    public String filename;
    @SerializedName("pid")
    public Long pid;
    @SerializedName("removedays")
    public Integer removedays;
    @SerializedName("cobraurl")
    public String cobraurl;

    public transient String action;

}
