package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.MockTool;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PromptRunRequest {

    /**
     * 会话ID（可选，如果提供则为持续对话，否则创建新会话）
     */
    private String sessionId;

    /**
     * Prompt Key（可选）
     */
    @Size(max = 255, message = "Prompt Key长度不能超过255个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Prompt Key只能包含字母、数字、下划线和短横线")
    private String promptKey;

    /**
     * 版本号（可选）
     */
    @Size(max = 32, message = "版本号长度不能超过32个字符")
    private String version;

    /**
     * Prompt内容（新会话时使用）
     */
    private String template;

    /**
     * Prompt中的变量值，JSON格式（新会话时使用）
     */
    private String variables;

    /**
     * 使用的模型相关参数，JSON格式（新会话时使用）
     */
    private String modelConfig;

    /**
     * 用户消息内容
     */
    @NotBlank(message = "用户消息不能为空")
    private String message;

    /**
     * 是否创建新会话（强制创建新会话，忽略sessionId）
     */
    private Boolean newSession;
    
    
    /**
     * 工具列表
     */
    private List<MockTool> mockTools;
}
