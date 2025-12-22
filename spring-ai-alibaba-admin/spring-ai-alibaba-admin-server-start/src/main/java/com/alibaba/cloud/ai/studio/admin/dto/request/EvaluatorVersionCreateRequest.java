package com.alibaba.cloud.ai.studio.admin.dto.request;

import lombok.Data;

@Data
public class EvaluatorVersionCreateRequest {

    /**
     * 评估器Id
     */
    private String evaluatorId;

    /**
     * 评估器版本描述
     */
    private String description;

    /**
     * 模型ID
     */
    private String modelConfig;

    /**
     * Prompt
     */
    private String prompt;


    /**
     * 评估器 版本号
     */

    private String version;



    /**
     * 版本状态
     */
    private String status;


    private String variables;

} 