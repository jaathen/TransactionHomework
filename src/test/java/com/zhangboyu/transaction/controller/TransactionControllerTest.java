package com.zhangboyu.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangboyu.transaction.converter.TransactionConverter;
import com.zhangboyu.transaction.dto.dto.PaginationDTO;
import com.zhangboyu.transaction.dto.dto.TransactionDTO;
import com.zhangboyu.transaction.dto.request.TransactionCreateRequest;
import com.zhangboyu.transaction.dto.request.TransactionUpdateRequest;
import com.zhangboyu.transaction.dto.response.PageData;
import com.zhangboyu.transaction.entity.Cursor;
import com.zhangboyu.transaction.entity.CursorPageResult;
import com.zhangboyu.transaction.entity.Transaction;
import com.zhangboyu.transaction.service.iface.TransactionService;
import com.zhangboyu.transaction.utils.TransactionUtils;
import com.zhangboyu.transaction.validator.TransactionValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private TransactionConverter transactionConverter;

    @MockitoBean
    private TransactionValidator transactionValidator;
    @Test
    void createTransaction() throws Exception {
        // 准备测试数据
        String serialNum = "1";
        TransactionCreateRequest request = TransactionUtils.createRequest(serialNum);
        // 设置其他必要字段...

        Transaction transaction = TransactionUtils.createTransaction();
        String transactionNo = transaction.getTransactionNo();
        TransactionDTO transactionDTO = new TransactionDTO();

        transactionDTO.setTransactionNo(transactionNo);
        // 模拟依赖行为
        Mockito.doNothing().when(transactionValidator).validate(request);
        Mockito.when(transactionConverter.toTransaction(request)).thenReturn(transaction);
        Mockito.when(transactionService.createTransaction(request.serialNumber(), transaction)).thenReturn(transactionNo);
        Mockito.when(transactionConverter.toTransactionDTO(transaction)).thenReturn(transactionDTO);

        // 执行请求并验证
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/transactions/" + transactionNo))
                .andExpect(jsonPath("$.data.transaction.transactionNo").value(transactionNo));

        // 验证依赖调用
        Mockito.verify(transactionValidator).validate(request);
        Mockito.verify(transactionService).createTransaction(serialNum, transaction);
    }

    @Test
    void updateTransaction() throws Exception {
        // 准备测试数据
        String transactionNo = "TX1001";
        TransactionUpdateRequest request = TransactionUtils.updateRequest(transactionNo);

        Transaction transaction = TransactionUtils.createTransaction();
        transaction.setTransactionNo(transactionNo);
        TransactionDTO transactionDTO = TransactionUtils.createTransactionDTO();
        transactionDTO.setTransactionNo(transactionNo);
        // 模拟依赖行为
        Mockito.doNothing().when(transactionValidator).validate(transactionNo);
        Mockito.when(transactionConverter.toTransaction(request)).thenReturn(transaction);
        Mockito.when(transactionConverter.toTransactionDTO(transaction)).thenReturn(transactionDTO);

        // 执行请求并验证
        mockMvc.perform(put("/api/v1/transactions/{transactionNo}", transactionNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transaction.transactionNo").value(transactionNo));

        // 验证依赖调用
        Mockito.verify(transactionService).updateTransaction(transaction);
    }

    @Test
    void getAllTransactions() throws Exception {
        // 准备分页数据
        String cursorString = "cursor123";
        Cursor cursor = new Cursor(new Date().getTime(), "");
        int pageSize = 20;
        CursorPageResult<Transaction> pageResult = new CursorPageResult<>();
        Transaction transaction = TransactionUtils.createTransaction();
        pageResult.setItems(List.of(transaction));
        String nextCursorString = "nextCursor";
        pageResult.setNextCursor(nextCursorString);
        pageResult.setHasNext(true);
        PageData<TransactionDTO> pageData = new PageData<>();
        TransactionDTO transactionDTO = TransactionUtils.createTransactionDTO();
        pageData.setItems(List.of(transactionDTO));
        PaginationDTO paginationDTO = new PaginationDTO();
        paginationDTO.setHasNext(pageResult.isHasNext());
        paginationDTO.setNextCursor(pageResult.getNextCursor());
        pageData.setPagination(paginationDTO);
        // 模拟依赖行为
        Mockito.when(transactionService.decodeCursor(cursorString)).thenReturn(cursor);
        Mockito.when(transactionService.listAllTransaction(cursor, pageSize)).thenReturn(pageResult);
        Mockito.when(transactionConverter.toTransactionDTOPageData(pageResult)).thenReturn(pageData);

        // 执行请求并验证
        mockMvc.perform(get("/api/v1/transactions")
                        .param("pageSize", "20")
                        .param("cursor", cursorString))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pagination.nextCursor").value(nextCursorString));
    }

    @Test
    void deleteTransaction() throws Exception {
        String transactionNo = "TX1001";

        mockMvc.perform(delete("/api/v1/transactions/{transactionNo}", transactionNo))
                .andExpect(status().isNoContent());
        Mockito.verify(transactionValidator).validate(transactionNo);
        Mockito.verify(transactionService).deleteTransaction(transactionNo);
    }
}