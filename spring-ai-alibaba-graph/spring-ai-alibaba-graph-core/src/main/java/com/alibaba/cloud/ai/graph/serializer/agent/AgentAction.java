package com.alibaba.cloud.ai.graph.serializer.agent;

import org.springframework.ai.chat.messages.AssistantMessage;

public record AgentAction(AssistantMessage.ToolCall toolCall, String log) {

}