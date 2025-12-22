package com.alibaba.cloud.ai.studio.admin.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Builder
@Data
public class EvaluatorVersionDO {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 评估器ID
     */
    private Long evaluatorId;

    /**
     * 评估器描述
     */
    private String description;

    /**
     * 版本号
     */
    private String version;

    /**
     * 模型ID
     */
    private String modelConfig;

    /**
     * Prompt配置（JSON格式）
     */
    private String prompt;

    /**
     * 评估器中的变量参数
     */
    private String variables;

    /**
     * 版本状态
     */
    private String status;

    /**
     * 实验集合（一对多关系）
     */
    private String experiments;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}