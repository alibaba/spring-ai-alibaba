package com.alibaba.cloud.ai.studio.admin.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluatorTestRequest {
    @NotNull
    private String modelConfig;

    @NotNull
    private String prompt;


    private String variables;

}
