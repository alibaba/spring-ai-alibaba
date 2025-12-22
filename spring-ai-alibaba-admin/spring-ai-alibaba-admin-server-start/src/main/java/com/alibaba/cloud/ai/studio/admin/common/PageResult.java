package com.alibaba.cloud.ai.studio.admin.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 总页数
     */
    private Long totalPage;

    /**
     * 当前页
     */
    private Long pageNumber;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 数据列表
     */
    private List<T> pageItems;

    /**
     * 构造函数
     */
    public PageResult() {
    }

    /**
     * 构造函数
     */
    public PageResult(Long totalCount, Long pageNumber, Long pageSize, List<T> pageItems) {
        this.totalCount = totalCount;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.pageItems = pageItems;
        this.totalPage = (totalCount + pageSize - 1) / pageSize;
    }
} 