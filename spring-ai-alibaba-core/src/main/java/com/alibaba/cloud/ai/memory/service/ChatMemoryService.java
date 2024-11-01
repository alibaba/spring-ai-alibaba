package com.alibaba.cloud.ai.memory.service;

import com.alibaba.cloud.ai.memory.adapter.MemoryAdapter;
import com.alibaba.cloud.ai.memory.adapter.ModelAdapter;
import com.alibaba.cloud.ai.memory.adapter.PersistenceAdapter;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Title conversation memory entry.<br>
 * Description memory operations for managing conversations .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Service
public class ChatMemoryService {

	@Autowired
	private ChatMemoryProperties properties;

	@Autowired
	private PersistenceAdapter persistenceAdapter;

	@Autowired
	private ModelAdapter modelAdapter;

	@Autowired
	private MemoryAdapter memoryAdapter;

	@PostConstruct
	public void initialize() {
		modelAdapter.init(properties.getModelName());
		memoryAdapter.init(properties.getMemoryType());
		persistenceAdapter.init(properties.getStorageType());
		persistenceAdapter.checkAndCreateTable();
	}

	/**
	 * start a conversation
	 */
	public void completion(String conversationId, String question) throws NoApiKeyException, InputRequiredException {
		List<ChatMessage> messages = persistenceAdapter.getMessages(conversationId, properties.getWindowSize());
		ChatMessage response = modelAdapter.getResponse(messages, conversationId, question, properties);
		memoryAdapter.addMessage(conversationId, response, question, persistenceAdapter, properties);
	}

	/**
	 * simple conversations do not use memory
	 */
	public ChatMessage simpleCompletion(String conversationId, String question)
			throws NoApiKeyException, InputRequiredException {
		return modelAdapter.getResponse(new ArrayList<>(), conversationId, question, properties);
	}

	/**
	 * 获取对话历史 windowSize = 0, get all Q&A pair windowSize > 0, Gets a Q&A pair of the
	 * specified size
	 */
	public List<ChatMessage> history(String conversationId, int windowSize) {
		return persistenceAdapter.getMessages(conversationId, windowSize);
	}

	/**
	 * Modify the conversation history based on the conversationId
	 */
	public void updateHistory(String conversationId, List<ChatMessage> messages) {
		persistenceAdapter.updateHistory(conversationId, messages);
	}

	/**
	 * Clears the conversation history of the conversationId
	 */
	public void clear(String conversationId) {
		persistenceAdapter.clearMessages(conversationId);
	}

}
