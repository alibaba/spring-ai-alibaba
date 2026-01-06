package com.alibaba.cloud.ai.studio.admin.dto.request;


import lombok.Data;

@Data
public class EvaluatorVersionUpdateRequest {
    /**
     * 主键ID
     */
    private Long evaluatorVersionId;


    /**
     * 评估器版本描述
     */
    private String description;

    private String status;


}
