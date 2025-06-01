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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis implementation of ChatMemoryRepository
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

	private static final String DEFAULT_KEY_PREFIX = "spring_ai_alibaba_chat_memory:";

	private final JedisPool jedisPool;

	private final ObjectMapper objectMapper;

	private RedisChatMemoryRepository(JedisPool jedisPool) {
		Assert.notNull(jedisPool, "jedisPool cannot be null");
		this.jedisPool = jedisPool;
		this.objectMapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(Message.class, new MessageDeserializer());
		this.objectMapper.registerModule(module);
	}

	public static RedisBuilder builder() {
		return new RedisBuilder();
	}

	public static class RedisBuilder {

		private String host = "127.0.0.1";

		private int port = 6379;

		private String password;

		private int timeout = 2000;

		private JedisPoolConfig poolConfig;

		public RedisBuilder host(String host) {
			this.host = host;
			return this;
		}

		public RedisBuilder port(int port) {
			this.port = port;
			return this;
		}

		public RedisBuilder password(String password) {
			this.password = password;
			return this;
		}

		public RedisBuilder timeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		public RedisBuilder poolConfig(JedisPoolConfig poolConfig) {
			this.poolConfig = poolConfig;
			return this;
		}

		public RedisChatMemoryRepository build() {
			if (poolConfig == null) {
				poolConfig = new JedisPoolConfig();
			}
			JedisPool jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
			return new RedisChatMemoryRepository(jedisPool);
		}

	}

	@Override
	public List<String> findConversationIds() {
		try (Jedis jedis = jedisPool.getResource()) {
			List<String> keys = new ArrayList<>(jedis.keys(DEFAULT_KEY_PREFIX + "*"));
			return keys.stream().map(key -> key.substring(DEFAULT_KEY_PREFIX.length())).toList();
		}
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try (Jedis jedis = jedisPool.getResource()) {
			String key = DEFAULT_KEY_PREFIX + conversationId;
			List<String> messageStrings = jedis.lrange(key, 0, -1);
			List<Message> messages = new ArrayList<>();

			for (String messageString : messageStrings) {
				try {
					Message message = objectMapper.readValue(messageString, Message.class);
					messages.add(message);
				}
				catch (JsonProcessingException e) {
					throw new RuntimeException("Error deserializing message", e);
				}
			}
			return messages;
		}
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		try (Jedis jedis = jedisPool.getResource()) {
			String key = DEFAULT_KEY_PREFIX + conversationId;
			// Clear existing messages first
			deleteByConversationId(conversationId);

			// Add all messages in order
			for (Message message : messages) {
				try {
					String messageJson = objectMapper.writeValueAsString(message);
					jedis.rpush(key, messageJson);
				}
				catch (JsonProcessingException e) {
					throw new RuntimeException("Error serializing message", e);
				}
			}
		}
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try (Jedis jedis = jedisPool.getResource()) {
			String key = DEFAULT_KEY_PREFIX + conversationId;
			jedis.del(key);
		}
	}

	/**
	 * Clear messages over the limit for a conversation
	 * @param conversationId the conversation ID
	 * @param maxLimit maximum number of messages to keep
	 * @param deleteSize number of messages to delete when over limit
	 */
	public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		try (Jedis jedis = jedisPool.getResource()) {
			String key = DEFAULT_KEY_PREFIX + conversationId;
			List<String> all = jedis.lrange(key, 0, -1);

			if (all.size() >= maxLimit) {
				all = all.stream().skip(Math.max(0, deleteSize)).toList();
				deleteByConversationId(conversationId);
				for (String message : all) {
					jedis.rpush(key, message);
				}
			}
		}
	}

	@Override
	public void close() {
		if (jedisPool != null) {
			jedisPool.close();
			logger.info("Redis connection pool closed");
		}
	}

}
