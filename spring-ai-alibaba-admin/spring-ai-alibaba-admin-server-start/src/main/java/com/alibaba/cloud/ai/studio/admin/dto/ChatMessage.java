package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    /**
     * 消息角色：user-用户，assistant-助手，system-系统
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息时间戳
     */
    private Long timestamp;
    
    
    /**
     * 消息指标
     */
    private ChatMessageMetrics metrics;
    
    /**
     * 创建用户消息
     */
    public static ChatMessage createUserMessage(String content) {
        return ChatMessage.builder().role("user").content(content).timestamp(System.currentTimeMillis()).build();
    }
    
    /**
     * 创建助手消息
     */
    public static ChatMessage createAssistantMessage(String content) {
        return ChatMessage.builder().role("assistant").content(content).timestamp(System.currentTimeMillis()).build();
    }
    
    /**
     * 创建系统消息
     */
    public static ChatMessage createSystemMessage(String content) {
        return ChatMessage.builder().role("system").content(content).timestamp(System.currentTimeMillis()).build();
    }
}
