package com.zhangboyu.transaction.exception;

import com.zhangboyu.transaction.enums.ErrorEnum;
import lombok.Getter;

@Getter
public class TransactionException extends RuntimeException {
    private final Integer code;
    private final String message;

    public TransactionException(ErrorEnum errorEnum) {
        this.code = errorEnum.getCode();
        this.message = errorEnum.getMessage();
    }

    public TransactionException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
