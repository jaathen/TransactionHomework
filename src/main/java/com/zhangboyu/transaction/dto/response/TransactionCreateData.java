package com.zhangboyu.transaction.dto.response;

import com.zhangboyu.transaction.dto.dto.TransactionDTO;
import lombok.Data;

@Data
public class TransactionCreateData {
    private TransactionDTO transaction;
}
