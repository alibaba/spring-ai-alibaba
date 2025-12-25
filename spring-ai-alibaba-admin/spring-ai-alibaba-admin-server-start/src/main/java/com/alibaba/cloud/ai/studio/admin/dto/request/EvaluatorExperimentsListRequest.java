package com.alibaba.cloud.ai.studio.admin.dto.request;

import lombok.Data;

@Data
public class EvaluatorExperimentsListRequest {
    /**
     * 页码
     */
    private Integer pageNumber = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 评估器ID
     */
    private Long evaluatorId;

}
