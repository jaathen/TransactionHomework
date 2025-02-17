package com.zhangboyu.transaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Date;

public record TransactionCreateRequest(
        @NotNull Long fromAccountId,
        @NotNull Long toAccountId,
        @NotNull @Positive Long amount,
        @NotBlank @Size(max = 3) String currency,

        String remark,
        @NotNull Integer status,
        @NotNull Integer type,

        @NotNull Long creator,

        Long createTime,

        @NotNull Long updater,

        Long updateTime,

        @NotBlank String serialNumber
) {}
