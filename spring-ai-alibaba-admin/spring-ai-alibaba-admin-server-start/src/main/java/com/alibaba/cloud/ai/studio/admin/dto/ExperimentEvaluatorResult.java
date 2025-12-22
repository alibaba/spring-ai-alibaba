package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExperimentEvaluatorResult {

    /**
     * 实验ID
     */
    private Long experimentId;


    /**
     * 评估平均得分（0.0-1.0）
     */
    private BigDecimal averageScore;


    /**
     * 评估器版本ID
     */
    private Long evaluatorVersionId;


    /**
     * 进度
     */
    private Integer progress;


    private Integer completeItemsCount;

    private Integer totalItemsCount;



} 