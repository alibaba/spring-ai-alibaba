/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.memory.memcached;

import com.alibaba.cloud.ai.memory.memcached.serializer.MessageDeserializer;
import com.alibaba.cloud.ai.toolcalling.memcached.MemcachedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memcached implementation of ChatMemoryRepository auth: dahua
 */
public class MemcachedChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(MemcachedChatMemoryRepository.class);

	private final MemcachedService memcachedService;

	private final ObjectMapper objectMapper;

	private static final String DEFAULT_CONVERSATION = "spring_ai_alibaba_chat_memory_conversation";

	private static final String DEFAULT_KEY_PREFIX = "spring_ai_alibaba_chat_memory:";

	public MemcachedChatMemoryRepository(MemcachedService memcachedService) {
		this.memcachedService = memcachedService;
		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
	}

	@Override
	public void close() {
		this.memcachedService.close();
	}

	@Override
	public List<String> findConversationIds() {
		return (List<String>) this.memcachedService.getter()
			.apply(new MemcachedService.MemcachedServiceGetter.Request(DEFAULT_CONVERSATION));
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Object apply = this.memcachedService.getter()
			.apply(new MemcachedService.MemcachedServiceGetter.Request(DEFAULT_KEY_PREFIX + conversationId));
		if (apply != null) {
			List<String> messageList = (List<String>) apply;
			return messageList.stream().map(messageStr -> {
				try {
					return objectMapper.readValue(messageStr, Message.class);
				}
				catch (JsonProcessingException e) {
					throw new RuntimeException("Error deserializing message", e);
				}
			}).toList();
		}
		return List.of();
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		List<String> conversationIds = findConversationIds() == null ? new ArrayList<>() : findConversationIds();
		// 保障消息顺序
		conversationIds.remove(conversationId);
		conversationIds.add(conversationId);
		this.memcachedService.setter()
			.apply(new MemcachedService.MemcachedServiceSetter.Request(DEFAULT_CONVERSATION, conversationIds, 0));
		List<String> serializingMessage = messages.stream().map(message -> {
			try {
				return this.objectMapper.writeValueAsString(message);
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("Error serializing message", e);
			}
		}).toList();
		this.memcachedService.setter()
			.apply(new MemcachedService.MemcachedServiceSetter.Request(DEFAULT_KEY_PREFIX + conversationId,
					serializingMessage, 0));
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		List<String> conversationIds = findConversationIds();
		conversationIds.remove(conversationId);
		this.memcachedService.setter()
			.apply(new MemcachedService.MemcachedServiceSetter.Request(DEFAULT_CONVERSATION, conversationIds, 0));
		this.memcachedService.deleter()
			.apply(new MemcachedService.MemcachedServiceDeleter.Request(DEFAULT_KEY_PREFIX + conversationId));
	}

	public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
		final int finalDeleteSize = deleteSize > maxLimit ? maxLimit : deleteSize;
		List<Message> messages = findByConversationId(conversationId);
		List<Message> lastMessages = new ArrayList<>();
		AtomicInteger index = new AtomicInteger(0);
		if (messages.size() >= maxLimit) {
			messages.stream().forEach(message -> {
				if (index.get() >= finalDeleteSize) {
					lastMessages.add(message);
				}
				index.incrementAndGet();
			});
		}
		saveAll(conversationId, lastMessages);
	}

}
