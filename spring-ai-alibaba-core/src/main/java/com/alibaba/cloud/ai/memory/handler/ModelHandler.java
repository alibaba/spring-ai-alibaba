package com.alibaba.cloud.ai.memory.handler;

import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.List;

/**
 * Title model type handler interface.<br>
 * Description Process user with different types of model for data interaction.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

public interface ModelHandler {

	ChatMessage getResponse(List<ChatMessage> messages, String conversationId, String question,
			ChatMemoryProperties properties) throws NoApiKeyException, InputRequiredException;

}