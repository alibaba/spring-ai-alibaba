package com.alibaba.cloud.ai.studio.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PromptUpdateRequest {

    /**
     * Prompt Key（唯一标识符）
     */
    @NotBlank(message = "Prompt Key不能为空")
    @Size(min = 1, max = 255, message = "Prompt Key长度必须在1-255个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Prompt Key只能包含字母、数字、下划线和短横线")
    private String promptKey;

    /**
     * Prompt描述
     */
    @Size(max = 255, message = "Prompt描述不能超过255个字符")
    private String promptDescription;

    /**
     * 标签，逗号分隔
     */
    @Size(max = 255, message = "标签总长度不能超过255个字符")
    @Pattern(regexp = "^[^,]+(,[^,]+)*$|^$", message = "标签格式不正确，应为逗号分隔的非空字符串")
    private String tags;
}
