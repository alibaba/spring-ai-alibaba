package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.ExperimentDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class Experiment {

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

    /**
     * 从DO对象转换为DTO对象
     *
     * @param experimentDO DO对象
     * @return DTO对象
     */
    public static Experiment fromDO(ExperimentDO experimentDO) {
        if (experimentDO == null) {
            return null;
        }
        return Experiment.builder()
                .id(experimentDO.getId())
                .name(experimentDO.getName())
                .description(experimentDO.getDescription())
                .datasetId(experimentDO.getDatasetId())
                .datasetVersion(experimentDO.getDatasetVersion())
                .evaluationObjectConfig(experimentDO.getEvaluationObjectConfig())
                .evaluatorConfig(experimentDO.getEvaluatorConfig())
                .status(experimentDO.getStatus())
                .progress(experimentDO.getProgress())
                .completeTime(experimentDO.getCompleteTime())
                .createTime(experimentDO.getCreateTime())
                .updateTime(experimentDO.getUpdateTime())
                .build();
    }

} 