package com.zhangboyu.transaction.dto.dto;

import lombok.Data;

@Data
public class PaginationDTO {
    private boolean hasNext;
    private String nextCursor;
}
