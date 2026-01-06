package com.alibaba.cloud.ai.studio.admin.dto.request;

import lombok.Data;

@Data
public class ModelConfigQueryRequest {

    /**
     * 页码，从1开始
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 模型名称（模糊查询）
     */
    private String name;

    /**
     * 提供商
     */
    private String provider;

    /**
     * 状态:1-启用,0-禁用
     */
    private Integer status;
}
