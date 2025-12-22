package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Data;

@Data
public class DatasetColumn {

    /**
     * 列名称
     */
    private String name;

    /**
     * 数据类型：STRING, NUMBER, BOOLEAN, JSON, ARRAY
     */
    private String dataType;

    /**
     * 显示格式：PLAIN_TEXT, MARKDOWN, CODE, JSON, TABLE
     */
    private String displayFormat;

    /**
     * 列描述
     */
    private String description;

    /**
     * 是否必填
     */
    private Boolean required;
} 