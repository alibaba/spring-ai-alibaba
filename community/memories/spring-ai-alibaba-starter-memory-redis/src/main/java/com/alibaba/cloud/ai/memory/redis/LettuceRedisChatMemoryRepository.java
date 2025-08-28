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

import com.alibaba.cloud.ai.memory.redis.builder.RedisChatMemoryBuilder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis implementation of ChatMemoryRepository using Lettuce
 *
 * @author benym
 * @since 2025/7/31 14:40
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

	public static class RedisBuilder extends RedisChatMemoryBuilder<RedisBuilder> {

		private GenericObjectPoolConfig<?> poolConfig;

		@Override
		protected RedisBuilder self() {
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
				lettuceConnectionFactory = new LettuceConnectionFactory(clusterConfig, applyConfiguration());
			}
			else {
				RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
				if (StringUtils.hasText(username)) {
					standaloneConfig.setUsername(username);
				}
				if (StringUtils.hasText(password)) {
					standaloneConfig.setPassword(password);
				}
				lettuceConnectionFactory = new LettuceConnectionFactory(standaloneConfig, applyConfiguration());
			}
			lettuceConnectionFactory.setShareNativeConnection(false);
			lettuceConnectionFactory.afterPropertiesSet();
			return new LettuceRedisChatMemoryRepository(lettuceConnectionFactory);
		}

		private LettuceClientConfiguration applyConfiguration() {
			// apply pool
			LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettucePoolingClientConfiguration
				.builder()
				.poolConfig(createDefaultPoolConfig());
			// apply timeout
			builder.commandTimeout(Duration.ofMillis(timeout));
			ClientOptions.Builder clientOptions = createClientOptions();
			// apply ssl
			if (useSsl && StringUtils.hasText(bundle)) {
				if (sslBundles == null) {
					throw new IllegalStateException(
							"spring.ssl configuration is required when use SSL in redis chat memory");
				}
				builder.useSsl();
				SslBundle sslBundle = sslBundles.getBundle(bundle);
				io.lettuce.core.SslOptions.Builder sslOptionsBuilder = io.lettuce.core.SslOptions.builder();
				sslOptionsBuilder.keyManager(sslBundle.getManagers().getKeyManagerFactory());
				sslOptionsBuilder.trustManager(sslBundle.getManagers().getTrustManagerFactory());
				SslOptions sslOptions = sslBundle.getOptions();
				if (sslOptions.getCiphers() != null) {
					sslOptionsBuilder.cipherSuites(sslOptions.getCiphers());
				}
				if (sslOptions.getEnabledProtocols() != null) {
					sslOptionsBuilder.protocols(sslOptions.getEnabledProtocols());
				}
				clientOptions.sslOptions(sslOptionsBuilder.build());
			}
			builder.clientOptions(clientOptions.build());
			return builder.build();
		}

		private GenericObjectPoolConfig<?> createDefaultPoolConfig() {
			if (poolConfig != null) {
				return poolConfig;
			}
			GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
			config.setMaxTotal(8);
			config.setMaxIdle(8);
			config.setMinIdle(2);
			return config;
		}

		private ClientOptions.Builder createClientOptions() {
			return ClientOptions.builder()
				.socketOptions(
						SocketOptions.builder().connectTimeout(Duration.ofMillis(timeout)).keepAlive(true).build())
				.autoReconnect(true)
				.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
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
