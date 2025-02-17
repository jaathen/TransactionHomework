package com.zhangboyu.transaction.entity;

import lombok.Data;

import java.util.List;

@Data
public class CursorPageResult <T> {
    private List<T> items;
    private boolean hasNext;
    private String nextCursor;
}
