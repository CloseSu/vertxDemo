package com.example.vertxdemo.request2;

import lombok.Data;

import java.util.Map;

@Data
public class RequestParams {

    //req
    private String absUrl;
    private Map<String,String> Header;
    private Map<String,String> Body;
    private String strBody;
    private boolean sendHeader;
    private boolean sendBody;

    //res
    private boolean backBody;
}
