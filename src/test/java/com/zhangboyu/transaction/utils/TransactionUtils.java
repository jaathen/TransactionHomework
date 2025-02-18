package com.zhangboyu.transaction.utils;


import com.zhangboyu.transaction.dto.dto.TransactionDTO;
import com.zhangboyu.transaction.dto.request.TransactionCreateRequest;
import com.zhangboyu.transaction.dto.request.TransactionUpdateRequest;
import com.zhangboyu.transaction.entity.Transaction;

import java.util.Date;
import java.util.UUID;

public class TransactionUtils {
    public static Transaction createTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionNo(UUID.randomUUID().toString());
        transaction.setFromAccountId(1L);
        transaction.setToAccountId(2L);
        transaction.setAmount(1000L);
        transaction.setCurrency("CNY");
        transaction.setRemark("remark");
        transaction.setType(1);
        transaction.setStatus(1);
        transaction.setCreateTime(new Date());
        transaction.setCreator(1000L);
        transaction.setUpdater(2000L);
        transaction.setUpdateTime(new Date());
        return transaction;
    }

    public static TransactionDTO createTransactionDTO() {
        TransactionDTO transaction = new TransactionDTO();
        transaction.setTransactionNo(UUID.randomUUID().toString());
        transaction.setFromAccountId(1L);
        transaction.setToAccountId(2L);
        transaction.setAmount(1000L);
        transaction.setCurrency("CNY");
        transaction.setRemark("remark");
        transaction.setType(1);
        transaction.setStatus(1);
        transaction.setCreateTime(new Date().getTime());
        transaction.setCreator(1000L);
        transaction.setUpdater(2000L);
        transaction.setUpdateTime(new Date().getTime());
        return transaction;
    }

    public static TransactionCreateRequest createRequest(String serialNum) {
        return new TransactionCreateRequest(1L,
                2L, 100L, "CNY", "remarkField",
                1, 1, 1L, new Date().getTime(), 1L, new Date().getTime(), serialNum);
    }

    public static TransactionUpdateRequest updateRequest(String transactionNo) {
        return new TransactionUpdateRequest(transactionNo, 1L,
                2L, 500L, "CNY", "remarkField",
                1, 1, 1L, new Date().getTime(), 1L, new Date().getTime());
    }
}
