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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import javax.net.ssl.SSLParameters;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis implementation of ChatMemoryRepository using Jedis
 *
 * @author Jast
 * @author benym
 */
public class JedisRedisChatMemoryRepository extends BaseRedisChatMemoryRepository {

	private static final Logger logger = LoggerFactory.getLogger(JedisRedisChatMemoryRepository.class);

	private final RedisConnectionFactory connectionFactory;

	private final RedisTemplate<String, String> redisTemplate;

	private JedisRedisChatMemoryRepository(RedisConnectionFactory connectionFactory) {
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

		private JedisPoolConfig poolConfig;

		@Override
		protected RedisBuilder self() {
			return this;
		}

		public JedisRedisChatMemoryRepository build() {
			JedisConnectionFactory jedisConnectionFactory;
			if (useCluster) {
				RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(Set.copyOf(nodes));
				if (StringUtils.hasText(username)) {
					clusterConfig.setUsername(username);
				}
				if (StringUtils.hasText(password)) {
					clusterConfig.setPassword(password);
				}
				jedisConnectionFactory = new JedisConnectionFactory(clusterConfig, applyConfiguration());
			}
			else {
				RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
				if (StringUtils.hasText(username)) {
					standaloneConfig.setUsername(username);
				}
				if (StringUtils.hasText(password)) {
					standaloneConfig.setPassword(password);
				}
				jedisConnectionFactory = new JedisConnectionFactory(standaloneConfig, applyConfiguration());
			}
			jedisConnectionFactory.afterPropertiesSet();
			return new JedisRedisChatMemoryRepository(jedisConnectionFactory);
		}

		private JedisPoolConfig getPoolConfigWithDefault() {
			return poolConfig != null ? poolConfig : new JedisPoolConfig();
		}

		private JedisClientConfiguration applyConfiguration() {
			// apply timeout
			JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
			builder.readTimeout(Duration.ofMillis(timeout)).connectTimeout(Duration.ofMillis(timeout));
			// apply ssl
			if (useSsl && StringUtils.hasText(bundle)) {
				JedisClientConfiguration.JedisSslClientConfigurationBuilder sslBuilder = builder.useSsl();
				if (sslBundles == null) {
					throw new IllegalStateException(
							"spring.ssl configuration is required when use SSL in redis chat memory");
				}
				SslBundle sslBundle = sslBundles.getBundle(bundle);
				sslBuilder.sslSocketFactory(sslBundle.createSslContext().getSocketFactory());
				SslOptions sslOptions = sslBundle.getOptions();
				SSLParameters sslParameters = new SSLParameters();
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(sslOptions.getCiphers()).to(sslParameters::setCipherSuites);
				map.from(sslOptions.getEnabledProtocols()).to(sslParameters::setProtocols);
				sslBuilder.sslParameters(sslParameters);
			}
			// apply pool
			builder.usePooling().poolConfig(getPoolConfigWithDefault());
			return builder.build();
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
		if (connectionFactory instanceof JedisConnectionFactory) {
			((JedisConnectionFactory) connectionFactory).destroy();
			logger.info("Jedis Redis connection pool closed");
		}
	}

}
