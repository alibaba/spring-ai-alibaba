package com.alibaba.cloud.ai.memory.handler;

import com.alibaba.cloud.ai.memory.adapter.PersistenceAdapter;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;

/**
 * Title memory type handler interface.<br>
 * Description Process conversation information with different types of memory for data
 * interaction.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

public interface MemoryHandler {

	void addMessage(String conversationId, ChatMessage message, String question, PersistenceAdapter persistenceAdapter,
			ChatMemoryProperties properties);

}