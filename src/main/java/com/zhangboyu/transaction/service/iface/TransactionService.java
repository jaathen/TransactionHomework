package com.zhangboyu.transaction.service.iface;

import com.zhangboyu.transaction.dto.dto.PageDTO;
import com.zhangboyu.transaction.entity.Transaction;

public interface TransactionService {
    String createTransaction(String serialNo, Transaction transaction);

    void deleteTransaction(String transaction);

     void updateTransaction(Transaction transaction);

     PageDTO<Transaction> listAllTransaction(int page, int size);

     String createTransactionNo(String serialNumber);

     boolean existBySerialNo(String serialNo);

    boolean existByTransactionNo(String transactionNo);
}
