package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
public class Evaluator {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 评估器名称
     */
    private String name;


    /**
     * 评估器描述
     */
    private String description;


    private String modelConfig;

    private String latestVersion;


    private String variables;

    private String prompt;


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
     * @param evaluatorDO DO对象
     * @return DTO对象
     */
    public static Evaluator fromDO(EvaluatorDO evaluatorDO) {
        if (evaluatorDO == null) {
            return null;
        }
        return Evaluator.builder()
                .id(evaluatorDO.getId())
                .name(evaluatorDO.getName())
                .description(evaluatorDO.getDescription())
                .createTime(evaluatorDO.getCreateTime())
                .updateTime(evaluatorDO.getUpdateTime())
                .build();
    }
} 