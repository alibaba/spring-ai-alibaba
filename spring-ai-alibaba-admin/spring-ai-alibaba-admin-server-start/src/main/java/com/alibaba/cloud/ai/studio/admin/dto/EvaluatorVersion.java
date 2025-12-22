package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorVersionDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Builder
@Data
public class EvaluatorVersion {
    /**
     * 主键ID
     */
    private Long id;


    /**
     * 评估器版本描述
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

    /**
     * 从DO对象转换为DTO对象
     *
     * @param evaluatorVersionDO DO对象
     * @return DTO对象
     */
    public static EvaluatorVersion fromDO(EvaluatorVersionDO evaluatorVersionDO) {
        if (evaluatorVersionDO == null) {
            return null;
        }
        return EvaluatorVersion.builder()
                .id(evaluatorVersionDO.getId())
                .description(evaluatorVersionDO.getDescription())
                .version(evaluatorVersionDO.getVersion())
                .modelConfig(evaluatorVersionDO.getModelConfig())
                .prompt(evaluatorVersionDO.getPrompt())
                .createTime(evaluatorVersionDO.getCreateTime())
                .updateTime(evaluatorVersionDO.getUpdateTime())
                .status(evaluatorVersionDO.getStatus())
                .experiments(evaluatorVersionDO.getExperiments())
                .build();
    }
}
