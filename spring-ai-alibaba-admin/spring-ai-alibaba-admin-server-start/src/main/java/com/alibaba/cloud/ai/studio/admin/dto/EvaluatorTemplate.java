package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorTemplateDO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluatorTemplate {
    private Long id;
    private String evaluatorTemplateKey;
    private String templateDesc;
    private String template;
    private String variables;
    private String modelConfig;

    public static EvaluatorTemplate fromDO(EvaluatorTemplateDO evaluatorTemplateDO) {
        if (evaluatorTemplateDO == null) {
            return null;
        }
        return EvaluatorTemplate.builder()
                .id(evaluatorTemplateDO.getId())
                .evaluatorTemplateKey(evaluatorTemplateDO.getEvaluatorTemplateKey())
                .templateDesc(evaluatorTemplateDO.getTemplateDesc())
                .template(evaluatorTemplateDO.getTemplate())
                .variables(evaluatorTemplateDO.getVariables())
                .modelConfig(evaluatorTemplateDO.getModelConfig())
                .build();
    }
}