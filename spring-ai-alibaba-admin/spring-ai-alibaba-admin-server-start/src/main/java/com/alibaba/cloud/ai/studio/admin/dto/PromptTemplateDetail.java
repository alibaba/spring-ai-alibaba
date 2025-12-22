package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.PromptTemplateDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateDetail {

    /**
     * Prompt模板名称
     */
    private String promptTemplateKey;

    /**
     * Prompt描述
     */
    private String templateDescription;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * Prompt内容
     */
    private String template;

    /**
     * Prompt中的变量值，JSON
     */
    private String variables;

    /**
     * 推荐使用的模型相关参数，JSON
     */
    private String modelConfig;

    /**
     * 从DO转换为DTO
     */
    public static PromptTemplateDetail fromDO(PromptTemplateDO promptTemplateDO) {
        if (promptTemplateDO == null) {
            return null;
        }
        return PromptTemplateDetail.builder()
                .promptTemplateKey(promptTemplateDO.getPromptTemplateKey())
                .templateDescription(promptTemplateDO.getTemplateDesc())
                .tags(promptTemplateDO.getTags())
                .template(promptTemplateDO.getTemplate())
                .variables(promptTemplateDO.getVariables())
                .modelConfig(promptTemplateDO.getModelConfig())
                .build();
    }
}
