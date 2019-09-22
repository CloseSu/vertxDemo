package com.example.vertxdemo.backup.vo;

import lombok.Data;

@Data
public class BackupFileVo {
    private String filename;
    private String time;
    private String path;
}
