package com.alibaba.cloud.ai.studio.admin.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExperimentResultDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 实验ID
     */
    private Long experimentId;

    /**
     * 输入内容
     */
    private String input;

    /**
     * 实际输出
     */
    private String actualOutput;

    /**
     * 参考输出
     */
    private String referenceOutput;

    /**
     * 评估得分（0.0-1.0）
     */
    private BigDecimal score;

    /**
     * 评估原因
     */
    private String reason;

    /**
     * 评估时间
     */
    private LocalDateTime evaluationTime;

    /**
     * 评估器版本ID
     */
    private Long evaluatorVersionId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 