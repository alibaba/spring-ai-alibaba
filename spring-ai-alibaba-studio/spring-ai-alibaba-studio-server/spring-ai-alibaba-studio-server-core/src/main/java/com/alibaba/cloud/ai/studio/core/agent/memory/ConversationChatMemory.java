/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.studio.core.agent.memory;

import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of ChatMemory that stores conversation history in Redis. Manages message
 * history with a maximum size limit and provides methods to add, retrieve, and clear
 * messages.
 *
 * @since 1.0.0.3
 */
@Component
public class ConversationChatMemory implements ChatMemory {

	/** Redis key prefix for conversation storage */
	public static String CONVERSATION_CHAT_MEMORY_PREFIX = "conversation_chat:%s";

	/** Redis manager for data persistence */
	private final RedisManager redisManager;

	/** Maximum number of messages to store in history */
	private final Integer maxMessages;

	public ConversationChatMemory(RedisManager redisManager, CommonConfig commonConfig) {
		this.redisManager = redisManager;
		this.maxMessages = commonConfig.getMaxConversationRoundInCache();
	}

	/**
	 * Adds new messages to the conversation history. If the history exceeds maxMessages,
	 * oldest messages are removed.
	 */
	@Override
	public void add(String conversationId, List<Message> messages) {
		String key = getConversationMemoryCacheKey(conversationId);
		Deque<Message> historyMessages = redisManager.get(key);

		if (Objects.isNull(historyMessages)) {
			historyMessages = new ArrayDeque<>();
		}

		for (Message message : messages) {
			if (messages.size() >= maxMessages) {
				historyMessages.poll();
			}

			historyMessages.offer(message);
		}

		redisManager.put(key, historyMessages);
	}

	/**
	 * Retrieves the last N messages from the conversation history.
	 * @return List of messages, empty if no history exists
	 */
	@Override
	public List<Message> get(String conversationId) {
		String key = getConversationMemoryCacheKey(conversationId);
		Deque<Message> all = redisManager.get(key);
		// FIXME, return only topN messages
		return all != null ? all.stream().toList() : List.of();
		// return all != null ? all.stream().skip(Math.max(0, all.size() -
		// lastN)).toList() : List.of();
	}

	/**
	 * Clears all messages for the given conversation.
	 */
	@Override
	public void clear(String conversationId) {
		String key = getConversationMemoryCacheKey(conversationId);
		redisManager.delete(key);
	}

	/**
	 * Generates the Redis key for storing conversation messages.
	 */
	private String getConversationMemoryCacheKey(String conversationId) {
		return String.format(CONVERSATION_CHAT_MEMORY_PREFIX, conversationId);
	}

}
