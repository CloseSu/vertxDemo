package com.example.vertxdemo.backup.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class ReturnListVo {

    @SerializedName("count")
    private Integer count;
    @SerializedName("list")
    private List<String> list;
}
