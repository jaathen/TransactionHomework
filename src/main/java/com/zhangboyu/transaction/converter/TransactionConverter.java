package com.zhangboyu.transaction.converter;

import com.zhangboyu.transaction.dto.dto.TransactionDTO;
import com.zhangboyu.transaction.dto.request.TransactionCreateRequest;
import com.zhangboyu.transaction.dto.request.TransactionUpdateRequest;
import com.zhangboyu.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

@Component
public class TransactionConverter {
    public Transaction toTransaction(TransactionCreateRequest request) {
        if (request == null) {
            return null;
        }
        Transaction transaction = new Transaction();
        transaction.setFromAccountId(request.fromAccountId());
        transaction.setToAccountId(request.toAccountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setRemark(request.remark());
        Date date = new Date();
        if (Objects.isNull(request.createTime())) {
            transaction.setCreateTime(date);
        } else {
            transaction.setCreateTime(new Date(request.createTime()));
        }
        if (Objects.isNull(request.updateTime())) {
            transaction.setUpdateTime(date);
        } else {
            transaction.setCreateTime(new Date(request.createTime()));
        }

        transaction.setCreator(request.creator());
        transaction.setUpdater(request.updater());
        return transaction;
    }

    public Transaction toTransaction(TransactionUpdateRequest request) {
        if (request == null) {
            return null;
        }
        Transaction transaction = new Transaction();
        transaction.setTransactionNo(request.transactionNo());
        transaction.setFromAccountId(request.fromAccountId());
        transaction.setToAccountId(request.toAccountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setRemark(request.remark());
        transaction.setType(request.type());
        transaction.setStatus(request.status());
        Date date = new Date();
        if (Objects.isNull(request.createTime())) {
            transaction.setCreateTime(date);
        } else {
            transaction.setCreateTime(new Date(request.createTime()));
        }
        if (Objects.isNull(request.updateTime())) {
            transaction.setUpdateTime(date);
        } else {
            transaction.setUpdateTime(new Date(request.createTime()));
        }

        transaction.setCreator(request.creator());
        transaction.setUpdater(request.updater());
        return transaction;
    }

    public TransactionDTO toTransactionDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionDTO dto = new TransactionDTO();
        dto.setTransactionNo(transaction.getTransactionNo());
        dto.setFromAccountId(transaction.getFromAccountId());
        dto.setToAccountId(transaction.getToAccountId());
        dto.setAmount(transaction.getAmount());
        dto.setCurrency(transaction.getCurrency());
        dto.setRemark(transaction.getRemark());
        dto.setType(transaction.getType());
        dto.setStatus(transaction.getStatus());
        dto.setCreateTime(transaction.getCreateTime().getTime());
        dto.setUpdateTime(transaction.getUpdateTime().getTime());
        dto.setCreator(transaction.getCreator());
        dto.setUpdater(transaction.getUpdater());
        return dto;
    }
}
