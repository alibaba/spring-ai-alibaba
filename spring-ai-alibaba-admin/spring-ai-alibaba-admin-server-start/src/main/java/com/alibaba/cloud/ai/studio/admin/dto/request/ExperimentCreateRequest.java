package com.alibaba.cloud.ai.studio.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ExperimentCreateRequest {

    /**
     * 实验名称
     */
    @NotBlank
    private String name;

    /**
     * 实验描述
     */
    private String description;

    /**
     * 数据集ID
     */
    @NotNull
    private Long datasetId;

    /**
     * 数据集版本
     */
    @NotBlank
    private Long datasetVersionId;

    @NotBlank
    private String datasetVersion;

    /**
     * 评测对象配置（JSON格式）
     */
    private String evaluationObjectConfig;



    /**
     * 评估器配置
     */
    private String evaluatorConfig;


}
