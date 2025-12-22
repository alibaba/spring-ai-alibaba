package com.alibaba.cloud.ai.studio.admin.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class DatasetItemUpdateRequest {

    /**
     * 数据项ID
     */
    @NotNull(message = "数据项ID不能为空")
    private Long id;

    /**
     * 数据内容（JSON格式）
     */
    @NotBlank(message = "数据内容不能为空")
    private String dataContent;

} 