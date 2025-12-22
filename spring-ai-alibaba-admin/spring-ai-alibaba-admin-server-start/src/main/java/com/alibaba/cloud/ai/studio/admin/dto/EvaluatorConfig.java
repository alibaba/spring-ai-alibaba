package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Data;

import java.util.List;


@Data
public class EvaluatorConfig {
    private Long evaluatorId;
    private Long evaluatorVersionId;
    private List<VariableMapItem> variableMap;
    private String evaluatorName;

}
