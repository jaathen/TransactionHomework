package com.zhangboyu.transaction.entity;

import lombok.Data;

import java.time.Instant;
import java.util.Date;

@Data
public class Transaction {
    //交易编号
    private String transactionNo;
    //资金转出账户id
    private Long fromAccountId;
    //资金转入账户id
    private Long toAccountId;
    //金额
    private Long amount;
    //币种
    private String currency;
    //备注
    private String remark;
    //交易类型
    private Integer type;
    //交易状态
    private Integer status;
    //创建时间
    private Date createTime;
    //创建人
    private Long creator;
    //更新时间
    private Date updateTime;
    //更新人
    private Long updater;
}
