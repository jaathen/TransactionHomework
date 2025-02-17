package com.zhangboyu.transaction.entity;

import lombok.Data;

@Data
public class Cursor {
    private Long date;

    private String transactionNo;

    public Cursor(Long date, String transactionNo) {
        this.date = date;
        this.transactionNo = transactionNo;
    }

    public static Cursor initial() {
        return new Cursor(0L, "");
    }
}
