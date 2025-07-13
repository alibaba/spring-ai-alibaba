package com.alibaba.cloud.ai.util;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

public class ChatResponseUtil {
    /**
     * 创建自定义状态响应
     *
     * @param statusMessage 状态消息
     * @return ChatResponse 状态响应对象
     */
    public static ChatResponse createCustomStatusResponse(String statusMessage) {
        statusMessage = statusMessage + "\n";
        AssistantMessage assistantMessage = new AssistantMessage(statusMessage);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(java.util.List.of(generation));
    }

}
