package com.alibaba.cloud.ai.studio.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "model_config")
public class ModelConfigDO {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模型名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 提供商(openai, azure, etc)
     */
    @Column(nullable = false, length = 50)
    private String provider;

    /**
     * 模型标识符(gpt-4, gpt-3.5-turbo等)
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * 模型服务地址
     */
    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    /**
     * API密钥
     */
    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    /**
     * 默认参数配置(JSON格式)
     */
    @Column(name = "default_parameters", columnDefinition = "JSON")
    private String defaultParameters;

    /**
     * 支持的参数定义(JSON格式)
     */
    @Column(name = "supported_parameters", columnDefinition = "JSON")
    private String supportedParameters;

    /**
     * 状态:1-启用,0-禁用
     */
    @Builder.Default
    private Integer status = 1;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
