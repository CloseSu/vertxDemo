package com.example.vertxdemo.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class AuthorizeVO {

    @SerializedName("access_key")
    public String accessKey;
    @SerializedName("secret_key")
    public String secretKey;
}
