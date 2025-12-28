package com.alibaba.cloud.ai.studio.admin.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExperimentEvaluatorResultDetailListRequest {
    @NotNull
    private Long experimentId;

    @NotNull
    private Long evaluatorVersionId;

    /**
     * 页码
     */
    private Integer pageNumber = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

}
