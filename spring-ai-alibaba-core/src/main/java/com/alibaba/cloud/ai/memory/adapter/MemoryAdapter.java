package com.alibaba.cloud.ai.memory.adapter;

import com.alibaba.cloud.ai.memory.chatmemory.BufferWindowMemory;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.handler.MemoryHandler;
import com.alibaba.cloud.ai.memory.persistence.ElasticsearchPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Title memory processing adapter.<br>
 * Description Used to accommodate processing of different memory types .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class MemoryAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchPersistence.class);

	@Autowired
	private BufferWindowMemory bufferWindowMemory;

	private MemoryHandler memoryHandler;

	public void init(String memoryType) {
		switch (memoryType.toLowerCase()) {
			case "bufferwindow" -> this.memoryHandler = bufferWindowMemory;
			default -> throw new IllegalArgumentException("Unsupported memory type: " + memoryType);
		}
	}

	public void addMessage(String conversationId, ChatMessage message, String question,
			PersistenceAdapter persistenceAdapter, ChatMemoryProperties properties) {
		Optional.ofNullable(message)
			.ifPresentOrElse(
					x -> memoryHandler.addMessage(conversationId, message, question, persistenceAdapter, properties),
					() -> logger.error("empty messages are not processed！"));
	}

}