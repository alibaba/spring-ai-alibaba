package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ModelConfigResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 提供商
     */
    private String provider;

    /**
     * 模型标识符
     */
    private String modelName;

    /**
     * 模型服务地址
     */
    private String baseUrl;

    /**
     * 默认参数配置
     */
    private Map<String, Object> defaultParameters;

    /**
     * 支持的参数定义
     */
    private List<ModelParameterDef> supportedParameters;

    /**
     * 状态:1-启用,0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
