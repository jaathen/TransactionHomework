package com.zhangboyu.transaction.dto.response;

import com.zhangboyu.transaction.dto.dto.PaginationDTO;
import lombok.Data;

import java.util.List;

@Data
public class PageData<T> {
    List<T> items;
    PaginationDTO pagination;
}
