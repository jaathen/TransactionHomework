package com.zhangboyu.transaction.controller;


import com.zhangboyu.transaction.converter.TransactionConverter;
import com.zhangboyu.transaction.dto.dto.PageDTO;
import com.zhangboyu.transaction.dto.dto.TransactionDTO;
import com.zhangboyu.transaction.dto.request.TransactionCreateRequest;
import com.zhangboyu.transaction.dto.request.TransactionUpdateRequest;
import com.zhangboyu.transaction.dto.response.BaseResponse;
import com.zhangboyu.transaction.dto.response.TransactionCreateData;
import com.zhangboyu.transaction.dto.response.TransactionResponse;
import com.zhangboyu.transaction.entity.Transaction;
import com.zhangboyu.transaction.service.iface.TransactionService;
import com.zhangboyu.transaction.validator.TransactionValidator;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transactions")
@Slf4j
public class TransactionController {
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private TransactionConverter transactionConverter;
    @Autowired
    private TransactionValidator transactionValidator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BaseResponse<TransactionCreateData>> createTransaction(@RequestBody @Valid TransactionCreateRequest request) {
        transactionValidator.validate(request);
        Transaction transaction = transactionConverter.toTransaction(request);
        String transactionNo = transactionService.createTransaction(request.serialNumber(), transaction);
        BaseResponse<TransactionCreateData> response = new BaseResponse<>();
        TransactionCreateData data = new TransactionCreateData();
        TransactionDTO transactionDTO = transactionConverter.toTransactionDTO(transaction);
        data.setTransaction(transactionDTO);
        response.setData(data);
        return ResponseEntity.created(URI.create("/transactions/" + transactionNo))
                .body(response);
    }

    @GetMapping
    public PageDTO<TransactionResponse> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
//        Page<Transaction> transactions = transactionService.listAllTransaction(page, size);
        return null;
    }

    @PutMapping("/{transactionNo}")
    public BaseResponse<TransactionCreateData> updateTransaction(
            @PathVariable String transactionNo,
            @Valid @RequestBody TransactionUpdateRequest request) {
        transactionValidator.validate(transactionNo);
        Transaction transaction = transactionConverter.toTransaction(request);
        transaction.setTransactionNo(transactionNo);
        transactionService.updateTransaction(transaction);
        BaseResponse<TransactionCreateData> response = new BaseResponse<>();
        TransactionCreateData data = new TransactionCreateData();
        TransactionDTO transactionDTO = transactionConverter.toTransactionDTO(transaction);
        data.setTransaction(transactionDTO);
        response.setData(data);
        return response;
    }

    // 删除交易
    @DeleteMapping("/{transactionNo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable @NotBlank String transactionNo) {
        transactionService.deleteTransaction(transactionNo);
    }
}
