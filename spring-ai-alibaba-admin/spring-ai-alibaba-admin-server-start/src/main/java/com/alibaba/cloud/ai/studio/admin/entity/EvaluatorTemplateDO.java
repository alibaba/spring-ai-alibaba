package com.alibaba.cloud.ai.studio.admin.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvaluatorTemplateDO {
    private Long id;
    private String evaluatorTemplateKey;
    private String templateDesc;
    private String template;
    private String variables;
    private String modelConfig;
}