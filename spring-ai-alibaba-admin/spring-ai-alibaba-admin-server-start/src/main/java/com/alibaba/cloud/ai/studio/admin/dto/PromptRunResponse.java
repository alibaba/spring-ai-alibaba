package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptRunResponse {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 是否为新会话
     */
    private Boolean newSession;
    
    /**
     * 消息内容（流式响应时使用）
     */
    private String content;
    
    /**
     * 响应类型：message-消息内容，session_info-会话信息，error-错误信息, metrics-指标信息
     */
    private String type;
    
    /**
     * 会话消息历史（会话信息时包含）
     */
    private List<ChatMessage> messages;
    
    /**
     * 消息总数
     */
    private Integer messageCount;
    
    /**
     * 错误信息（错误时使用）
     */
    private String error;
    
    /**
     * 指标信息（metrics 时包含）
     */
    private ChatMessageMetrics metrics;
    
    /**
     * 创建消息响应
     */
    public static PromptRunResponse createMessageResponse(String sessionId, String content) {
        return PromptRunResponse.builder().sessionId(sessionId).content(content).type("message").build();
    }
    
    /**
     * 创建会话信息响应
     */
    public static PromptRunResponse createSessionInfoResponse(ChatSession session) {
        return PromptRunResponse.builder().sessionId(session.getSessionId()).newSession(session.isNewSession())
                .type("session_info").messages(session.getMessages()).messageCount(session.getMessageCount()).build();
    }
    
    /**
     * 创建错误响应
     */
    public static PromptRunResponse createErrorResponse(String sessionId, String error) {
        return PromptRunResponse.builder().sessionId(sessionId).error(error).type("error").build();
    }
    
    public static PromptRunResponse createMetricsResponse(String sessionId, ChatMessageMetrics metrics) {
        return PromptRunResponse.builder().sessionId(sessionId).type("metrics").metrics(metrics).build();
    }
}
