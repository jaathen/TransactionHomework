package com.zhangboyu.transaction.dto.response;

import lombok.Data;

@Data
public class ErrorResponse  {
    private final long timestamp = System.currentTimeMillis();
    private final int code;
    private final String message;
    private final Object details;
}