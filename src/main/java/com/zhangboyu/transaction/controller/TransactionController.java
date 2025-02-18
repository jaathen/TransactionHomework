package com.zhangboyu.transaction.controller;


import com.zhangboyu.transaction.converter.TransactionConverter;
import com.zhangboyu.transaction.dto.dto.TransactionDTO;
import com.zhangboyu.transaction.dto.request.TransactionCreateRequest;
import com.zhangboyu.transaction.dto.request.TransactionUpdateRequest;
import com.zhangboyu.transaction.dto.response.BaseResponse;
import com.zhangboyu.transaction.dto.response.PageData;
import com.zhangboyu.transaction.dto.response.TransactionCreateData;
import com.zhangboyu.transaction.entity.CursorPageResult;
import com.zhangboyu.transaction.entity.Transaction;
import com.zhangboyu.transaction.service.iface.TransactionService;
import com.zhangboyu.transaction.validator.TransactionValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    public BaseResponse<PageData<TransactionDTO>> getAllTransactions(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String cursor) {
        CursorPageResult<Transaction> transactionCursorPageResult = transactionService.listAllTransaction(transactionService.decodeCursor(cursor), pageSize);
        PageData<TransactionDTO> transactionDTOPageData = transactionConverter.toTransactionDTOPageData(transactionCursorPageResult);
        BaseResponse<PageData<TransactionDTO>> response = new BaseResponse<>();
        response.setData(transactionDTOPageData);
        return response;
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
        transactionValidator.validate(transactionNo);
        transactionService.deleteTransaction(transactionNo);
    }
}
