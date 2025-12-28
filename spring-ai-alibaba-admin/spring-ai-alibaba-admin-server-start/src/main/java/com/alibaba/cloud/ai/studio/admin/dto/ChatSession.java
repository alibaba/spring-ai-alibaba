package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Prompt Key
     */
    private String promptKey;

    /**
     * Prompt版本
     */
    private String version;

    /**
     * Prompt模板
     */
    private String template;

    /**
     * 变量配置（JSON字符串）
     */
    private String variables;

    /**
     * 模型配置（JSON字符串）
     */
    private ModelConfigInfo modelConfig;

    /**
     * 会话消息历史
     */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    
    @Builder.Default
    private List<MockTool> mockTools = new ArrayList<>();

    /**
     * 会话创建时间
     */
    private Long createTime;

    /**
     * 最后更新时间
     */
    private Long lastUpdateTime;

    /**
     * 添加消息到会话
     */
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        addMessage(ChatMessage.createUserMessage(content));
    }

    /**
     * 添加助手消息
     */
    public void addAssistantMessage(String content) {
        addMessage(ChatMessage.createAssistantMessage(content));
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * 是否为新会话（没有消息历史）
     */
    public boolean isNewSession() {
        return messages.isEmpty();
    }
}
