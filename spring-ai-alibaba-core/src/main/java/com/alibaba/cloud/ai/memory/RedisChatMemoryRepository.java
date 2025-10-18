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
package com.alibaba.cloud.ai.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Libres-coder
 */
public class RedisChatMemoryRepository implements ChatMemory {

	private static final Logger logger = LoggerFactory.getLogger(RedisChatMemoryRepository.class);

	private static final String KEY_PREFIX = "spring:ai:chat:memory:";

	private final JedisPool jedisPool;

	private final ObjectMapper objectMapper;

	public RedisChatMemoryRepository(JedisPool jedisPool) {
		this(jedisPool, new ObjectMapper());
	}

	public RedisChatMemoryRepository(JedisPool jedisPool, ObjectMapper objectMapper) {
		this.jedisPool = jedisPool;
		this.objectMapper = objectMapper;
		this.objectMapper.findAndRegisterModules();
	}

	@Override
	public void add(String conversationId, List<Message> messages) {
		String key = getKey(conversationId);

		try (Jedis jedis = jedisPool.getResource()) {
			List<Message> existingMessages = doGet(jedis, key);
			existingMessages.addAll(messages);
			String json = objectMapper.writeValueAsString(existingMessages);
			jedis.set(key, json);
		}
		catch (JsonProcessingException e) {
			logger.error("Failed to serialize messages for conversation: {}", conversationId, e);
			throw new RuntimeException("Failed to serialize messages", e);
		}
	}

	@Override
	public List<Message> get(String conversationId) {
		String key = getKey(conversationId);

		try (Jedis jedis = jedisPool.getResource()) {
			return doGet(jedis, key);
		}
	}

	@Override
	public void clear(String conversationId) {
		String key = getKey(conversationId);

		try (Jedis jedis = jedisPool.getResource()) {
			jedis.del(key);
		}
	}

	private List<Message> doGet(Jedis jedis, String key) {
		String json = jedis.get(key);
		if (json == null || json.isEmpty()) {
			return new ArrayList<>();
		}

		try {
			return objectMapper.readValue(json, new TypeReference<List<Message>>() {
			});
		}
		catch (IOException e) {
			logger.error("Failed to deserialize messages from key: {}", key, e);
			return new ArrayList<>();
		}
	}

	private String getKey(String conversationId) {
		return KEY_PREFIX + conversationId;
	}

	public static class Builder {

		private String host = "localhost";

		private int port = 6379;

		private int timeout = 2000;

		private String password;

		private int database = 0;

		private redis.clients.jedis.JedisPoolConfig poolConfig;

		public Builder host(String host) {
			this.host = host;
			return this;
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		public Builder timeout(int timeout) {
			this.timeout = timeout;
			return this;
		}

		public Builder password(String password) {
			this.password = password;
			return this;
		}

		public Builder database(int database) {
			this.database = database;
			return this;
		}

		public Builder poolConfig(redis.clients.jedis.JedisPoolConfig poolConfig) {
			this.poolConfig = poolConfig;
			return this;
		}

		public RedisChatMemoryRepository build() {
			if (this.poolConfig == null) {
				this.poolConfig = new redis.clients.jedis.JedisPoolConfig();
			}

			JedisPool jedisPool;
			if (password != null && !password.isEmpty()) {
				jedisPool = new JedisPool(this.poolConfig, this.host, this.port, this.timeout, this.password,
						this.database);
			}
			else {
				jedisPool = new JedisPool(this.poolConfig, this.host, this.port, this.timeout, null, this.database);
			}

			return new RedisChatMemoryRepository(jedisPool);
		}

	}

}

