package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Data;

@Data
public class ModelParameterDef {

    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型: number, string, boolean
     */
    private String type;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 最小值(数字类型)
     */
    private Object minValue;

    /**
     * 最大值(数字类型)
     */
    private Object maxValue;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 是否必填
     */
    private Boolean required;
}
