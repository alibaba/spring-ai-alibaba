package com.alibaba.cloud.ai.studio.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PromptVersionCreateRequest {

    /**
     * Prompt Key
     */
    @NotBlank(message = "Prompt Key不能为空")
    @Size(min = 1, max = 255, message = "Prompt Key长度必须在1-255个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Prompt Key只能包含字母、数字、下划线和短横线")
    private String promptKey;

    /**
     * 版本号
     */
    @NotBlank(message = "版本号不能为空")
    @Size(min = 1, max = 32, message = "版本号长度必须在1-32个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "版本号只能包含字母、数字、点、下划线和短横线")
    private String version;

    /**
     * 版本描述
     */
    @Size(max = 255, message = "版本描述不能超过255个字符")
    private String versionDescription;

    /**
     * Prompt内容
     */
    @NotBlank(message = "Prompt内容不能为空")
    private String template;

    /**
     * Prompt中的变量值，JSON格式
     */
    private String variables;

    /**
     * 使用的模型相关参数，JSON格式
     */
    private String modelConfig;

    /**
     * 版本状态：pre-预发布版本，release-正式版本
     */
    @Pattern(regexp = "^(pre|release)$", message = "版本状态必须是pre或release")
    private String status = "pre";
}
