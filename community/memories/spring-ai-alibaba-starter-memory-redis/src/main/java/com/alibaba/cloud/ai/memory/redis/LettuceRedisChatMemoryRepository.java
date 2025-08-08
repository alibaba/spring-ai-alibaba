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

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis implementation of ChatMemoryRepository using Lettuce
 *
 * @author benym
 * @date 2025/7/31 14:40
 */
public class LettuceRedisChatMemoryRepository extends BaseRedisChatMemoryRepository {

	private static final Logger logger = LoggerFactory.getLogger(LettuceRedisChatMemoryRepository.class);

	private final RedisConnectionFactory connectionFactory;

	private final RedisTemplate<String, String> redisTemplate;

	private LettuceRedisChatMemoryRepository(RedisConnectionFactory connectionFactory) {
		super();
		Assert.notNull(connectionFactory, "ConnectionFactory cannot be null");
		this.connectionFactory = connectionFactory;
		this.redisTemplate = createRedisTemplate(connectionFactory);
	}

	private RedisTemplate<String, String> createRedisTemplate(RedisConnectionFactory connectionFactory) {
		StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		template.afterPropertiesSet();
		return template;
	}

	public static RedisBuilder builder() {
		return new RedisBuilder();
	}

	public static class RedisBuilder {

		private String host = "127.0.0.1";

		/**
		 * example 127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
		 */
		private List<String> nodes = new ArrayList<>();

		private int port = 6379;

		private String username;

		private String password;

		private int timeout = 2000;

		private GenericObjectPoolConfig<?> poolConfig;

		private boolean useCluster = false;

		public RedisBuilder host(String host) {
			this.host = host;
			return this;
		}

		public RedisBuilder nodes(List<String> nodes) {
			this.nodes = nodes;
			this.useCluster = true;
			return this;
		}

		public RedisBuilder port(int port) {
			this.port = port;
			return this;
		}

		public RedisBuilder username(String username) {
			this.username = username;
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

		public RedisBuilder poolConfig(GenericObjectPoolConfig<?> poolConfig) {
			this.poolConfig = poolConfig;
			return this;
		}

		public LettuceRedisChatMemoryRepository build() {
			LettuceConnectionFactory lettuceConnectionFactory;
			if (useCluster) {
				RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(Set.copyOf(nodes));
				if (StringUtils.hasText(username)) {
					clusterConfig.setUsername(username);
				}
				if (StringUtils.hasText(password)) {
					clusterConfig.setPassword(password);
				}
				LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientBuilder = LettucePoolingClientConfiguration
					.builder()
					.commandTimeout(Duration.ofMillis(timeout))
					.poolConfig(poolConfig != null ? poolConfig : createDefaultPoolConfig())
					.clientOptions(createClientOptions());
				lettuceConnectionFactory = new LettuceConnectionFactory(clusterConfig, clientBuilder.build());
			}
			else {
				RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
				if (StringUtils.hasText(username)) {
					standaloneConfig.setUsername(username);
				}
				if (StringUtils.hasText(password)) {
					standaloneConfig.setPassword(password);
				}
				LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder clientBuilder = LettucePoolingClientConfiguration
					.builder()
					.commandTimeout(Duration.ofMillis(timeout))
					.poolConfig(poolConfig != null ? poolConfig : createDefaultPoolConfig())
					.clientOptions(createClientOptions());
				lettuceConnectionFactory = new LettuceConnectionFactory(standaloneConfig, clientBuilder.build());
			}
			lettuceConnectionFactory.setShareNativeConnection(false);
			lettuceConnectionFactory.afterPropertiesSet();
			return new LettuceRedisChatMemoryRepository(lettuceConnectionFactory);
		}

		private GenericObjectPoolConfig<?> createDefaultPoolConfig() {
			GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
			config.setMaxTotal(8);
			config.setMaxIdle(8);
			config.setMinIdle(2);
			return config;
		}

		private ClientOptions createClientOptions() {
			return ClientOptions.builder()
				.socketOptions(
						SocketOptions.builder().connectTimeout(Duration.ofMillis(timeout)).keepAlive(true).build())
				.autoReconnect(true)
				.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
				.build();
		}

	}

	@Override
	public List<String> findConversationIds() {
		Set<String> keys = redisTemplate.keys(DEFAULT_KEY_PREFIX + "*");
		return keys.stream().map(key -> key.substring(DEFAULT_KEY_PREFIX.length())).collect(Collectors.toList());
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		String key = DEFAULT_KEY_PREFIX + conversationId;
		List<String> messageStrings = redisTemplate.opsForList().range(key, 0, -1);
		if (CollectionUtils.isEmpty(messageStrings)) {
			return Collections.emptyList();
		}
		return messageStrings.stream().map(this::deserializeMessage).collect(Collectors.toList());
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");
		String key = DEFAULT_KEY_PREFIX + conversationId;
		List<String> messageJsons = messages.stream().map(this::serializeMessage).toList();
		try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
			connection.keyCommands().del(key.getBytes());
			if (!messageJsons.isEmpty()) {
				byte[][] values = new byte[messageJsons.size()][];
				for (int i = 0; i < messageJsons.size(); i++) {
					values[i] = messageJsons.get(i).getBytes();
				}
				connection.listCommands().rPush(key.getBytes(), values);
			}
		}
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		redisTemplate.delete(DEFAULT_KEY_PREFIX + conversationId);
	}

	/**
	 * Clear messages over the limit for a conversation
	 * @param conversationId the conversation ID
	 * @param maxLimit maximum number of messages to keep
	 * @param deleteSize number of messages to delete when over limit
	 */
	public void clearOverLimit(String conversationId, int maxLimit, int deleteSize) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		String key = DEFAULT_KEY_PREFIX + conversationId;
		Long size = redisTemplate.opsForList().size(key);
		if (size < maxLimit) {
			return;
		}
		redisTemplate.opsForList().trim(key, deleteSize, -1);
	}

	@Override
	public void close() {
		if (connectionFactory instanceof LettuceConnectionFactory) {
			((LettuceConnectionFactory) connectionFactory).destroy();
			logger.info("Lettuce Redis connection pool closed");
		}
	}

}
