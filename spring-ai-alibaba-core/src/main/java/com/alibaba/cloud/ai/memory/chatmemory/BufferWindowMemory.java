package com.alibaba.cloud.ai.memory.chatmemory;

import com.alibaba.cloud.ai.memory.adapter.PersistenceAdapter;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.enums.RoleTypeEnum;
import com.alibaba.cloud.ai.memory.handler.MemoryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Title fixed window size of memory.<br>
 * Description Handles memory logic of fixed window size .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class BufferWindowMemory implements MemoryHandler {

	@Override
	public void addMessage(String conversationId, ChatMessage message, String question,
			PersistenceAdapter persistenceAdapter, ChatMemoryProperties properties) {
		ChatMessage inputMessage = new ChatMessage();
		inputMessage.setRole(RoleTypeEnum.USER.getRoleName());
		inputMessage.setContent(question);
		inputMessage.setInputTokens(message.getInputTokens());
		inputMessage.setCreatedAt(System.currentTimeMillis());
		message.setCreatedAt(System.currentTimeMillis());
		List<ChatMessage> messages = persistenceAdapter.getMessages(conversationId, 0);
		messages.add(inputMessage);
		messages.add(message);
		persistenceAdapter.saveMessage(conversationId, messages);
	}

}