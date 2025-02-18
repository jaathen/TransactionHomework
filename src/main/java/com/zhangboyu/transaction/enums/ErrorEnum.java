package com.zhangboyu.transaction.enums;

public enum ErrorEnum {


    CREATE_DUPLICATE_EXCEPTION(101, "重复创建交易"),
    TRANSACTION_NOT_EXISTS_EXCEPTION(102, "交易不存在"),

    CURSOR_EXCEPTION(103, "游标异常"),

    SYSTEM_EXCEPTION(500, "系统异常请稍后重试"),
    CONCURRENCY_EXCEPTION(600, "并发,请稍后重试"),
    ARGUMENT_EXCEPTION(4001, "参数异常"),
    ;
    ;
    private final int code;
    private final String message;

    ErrorEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
