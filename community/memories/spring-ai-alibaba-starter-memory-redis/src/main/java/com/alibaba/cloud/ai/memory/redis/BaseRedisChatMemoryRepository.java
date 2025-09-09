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
package com.alibaba.cloud.ai.memory.redis;

import com.alibaba.cloud.ai.memory.redis.serializer.MessageDeserializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

/**
 * Base class for Redis-based chat memory repositories
 *
 * @author benym
 * @since 2025/7/31 0:05
 */
public abstract class BaseRedisChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

	protected static final Logger logger = LoggerFactory.getLogger(BaseRedisChatMemoryRepository.class);

	protected static final String DEFAULT_KEY_PREFIX = "spring_ai_alibaba_chat_memory:";

	protected final ObjectMapper objectMapper;

	public BaseRedisChatMemoryRepository() {
		this.objectMapper = JsonMapper.builder()
			.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
			.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
			.visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
			.build();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
	}

	protected Message deserializeMessage(String messageStr) {
		try {
			return objectMapper.readValue(messageStr, Message.class);
		}
		catch (JsonProcessingException e) {
			logger.error("Deserialization error for message: {}", messageStr, e);
			return null;
		}
	}

	protected String serializeMessage(Message message) {
		try {
			return objectMapper.writeValueAsString(message);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Error serializing message", e);
		}
	}

}
