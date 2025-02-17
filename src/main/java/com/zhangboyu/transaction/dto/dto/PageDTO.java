package com.zhangboyu.transaction.dto.dto;

import java.util.List;

public class PageDTO<T> {
    private List<T> items;

    private Long pageNo;

    private Long pageSize;

    private Long total;

    private boolean isEnd;
}
