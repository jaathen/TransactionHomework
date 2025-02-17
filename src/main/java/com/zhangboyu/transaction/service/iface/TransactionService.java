package com.zhangboyu.transaction.service.iface;

import com.zhangboyu.transaction.dto.dto.PageDTO;
import com.zhangboyu.transaction.entity.Cursor;
import com.zhangboyu.transaction.entity.CursorPageResult;
import com.zhangboyu.transaction.entity.Transaction;

public interface TransactionService {
    String createTransaction(String serialNo, Transaction transaction);

    void deleteTransaction(String transaction);

     void updateTransaction(Transaction transaction);

    CursorPageResult<Transaction> listAllTransaction(Cursor cursor, int pageSize);

     String createTransactionNo(String serialNumber);

     boolean existBySerialNo(String serialNo);

    boolean existByTransactionNo(String transactionNo);

    Cursor decodeCursor(String encoded);

    String encodeCursor(Transaction lastItem);
}
