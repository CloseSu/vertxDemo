package com.example.vertxdemo.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DataVO {

    @SerializedName("key")
    public String key;
    @SerializedName("hash")
    public String hash;
    public transient String path;
}
