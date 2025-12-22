package com.alibaba.cloud.ai.studio.admin.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExperimentDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 实验名称
     */
    private String name;

    /**
     * 实验描述
     */
    private String description;

    /**
     * 数据集ID
     */
    private Long datasetId;

    /**
     * 数据集版本
     */
    private Long datasetVersionId;


    /**
     * 数据集版本号
     */
    private String datasetVersion;

    /**
     * 评测对象配置（JSON格式）
     */
    private String evaluationObjectConfig;


    /**
     * 评估器配置
     */
    private String evaluatorConfig;

    /**
     * 状态：DRAFT-草稿，RUNNING-运行中，COMPLETED-已完成，FAILED-失败，STOPPED-已停止
     */
    private String status;

    /**
     * 进度百分比：0-100
     */
    private Integer progress;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}