package com.zhangboyu.transaction.validator;

import com.zhangboyu.transaction.dto.request.TransactionCreateRequest;
import com.zhangboyu.transaction.enums.ErrorEnum;
import com.zhangboyu.transaction.exception.TransactionException;
import com.zhangboyu.transaction.service.iface.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransactionValidator {
    @Autowired
    private TransactionService transactionService;
    public void validate(TransactionCreateRequest request) {

        if (transactionService.existBySerialNo(request.serialNumber())) {
            throw new TransactionException(ErrorEnum.CREATE_DUPLICATE_EXCEPTION);
        }
    }

    public void validate(String transactionNo) {
        if (!transactionService.existByTransactionNo(transactionNo)) {
            throw new TransactionException(ErrorEnum.TRANSACTION_NOT_EXISTS_EXCEPTION);
        }
    }
}
