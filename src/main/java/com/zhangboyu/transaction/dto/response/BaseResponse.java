package com.zhangboyu.transaction.dto.response;


import lombok.Data;

@Data
public class BaseResponse <T> {
    private int code = 200;
    private String message = "success";

    private T data;
}
