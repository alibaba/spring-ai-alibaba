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
import org.redisson.Redisson;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Redis implementation of ChatMemoryRepository using Redisson
 *
 * @author benym
 * @since 2025/7/30 18:47
 */
public class RedissonRedisChatMemoryRepository extends BaseRedisChatMemoryRepository {

	private static final Logger logger = LoggerFactory.getLogger(RedissonRedisChatMemoryRepository.class);

	private final RedissonClient redissonClient;

	private RedissonRedisChatMemoryRepository(RedissonClient redissonClient) {
		super();
		Assert.notNull(redissonClient, "redissonClient cannot be null");
		this.redissonClient = redissonClient;
	}

	public static RedissonBuilder builder() {
		return new RedissonBuilder();
	}

	public static class RedissonBuilder extends RedisChatMemoryBuilder<RedissonBuilder> {

		private int poolSize = 32;

		private Config redissonConfig;

		@Override
		protected RedissonBuilder self() {
			return this;
		}

		public RedissonBuilder poolSize(int poolSize) {
			this.poolSize = poolSize;
			return this;
		}

		public RedissonBuilder redissonConfig(Config redissonConfig) {
			this.redissonConfig = redissonConfig;
			return this;
		}

		public RedissonRedisChatMemoryRepository build() {
			if (redissonConfig != null) {
				// when the user does not set redisson serialization, maintain String
				// serialization consistent with jedis and lettuce
				if (redissonConfig.getCodec() == null) {
					redissonConfig.setCodec(new StringCodec());
				}
				return new RedissonRedisChatMemoryRepository(Redisson.create(redissonConfig));
			}
			Config config = new Config();
			config.setCodec(new StringCodec());
			if (useCluster) {
				List<String> nodesUrl = nodes.stream().map(node -> "redis://" + node).toList();
				if (useSsl && StringUtils.hasText(bundle)) {
					if (sslBundles == null) {
						throw new IllegalStateException(
								"spring.ssl configuration is required when use SSL in redis chat memory");
					}
					SslBundle sslBundle = sslBundles.getBundle(bundle);
					SslManagerBundle managers = sslBundle.getManagers();
					config.useClusterServers().setSslTrustManagerFactory(managers.getTrustManagerFactory());
					nodesUrl = nodes.stream().map(node -> "rediss://" + node).toList();
				}
				config.useClusterServers()
					.addNodeAddress(nodesUrl.toArray(new String[0]))
					.setConnectTimeout(timeout)
					.setSlaveConnectionPoolSize(poolSize)
					.setMasterConnectionPoolSize(poolSize);
				if (StringUtils.hasLength(username)) {
					config.useClusterServers().setUsername(username);
				}
				if (StringUtils.hasLength(password)) {
					config.useClusterServers().setPassword(password);
				}
			}
			else {
				String nodeUrl = "redis://" + host + ":" + port;
				if (useSsl && StringUtils.hasText(bundle)) {
					if (sslBundles == null) {
						throw new IllegalStateException(
								"spring.ssl configuration is required when use SSL in redis chat memory");
					}
					SslBundle sslBundle = sslBundles.getBundle(bundle);
					SslManagerBundle managers = sslBundle.getManagers();
					config.useSingleServer().setSslTrustManagerFactory(managers.getTrustManagerFactory());
					nodeUrl = "rediss://" + host + ":" + port;
				}
				config.useSingleServer().setAddress(nodeUrl).setConnectionPoolSize(poolSize).setConnectTimeout(timeout);
				if (StringUtils.hasLength(username)) {
					config.useSingleServer().setUsername(username);
				}
				if (StringUtils.hasLength(password)) {
					config.useSingleServer().setPassword(password);
				}
			}
			return new RedissonRedisChatMemoryRepository(Redisson.create(config));

		}

	}

	@Override
	public List<String> findConversationIds() {
		RKeys keys = redissonClient.getKeys();
		KeysScanOptions scanOptions = KeysScanOptions.defaults().pattern(DEFAULT_KEY_PREFIX + "*");
		Iterable<String> keysIter = keys.getKeys(scanOptions);
		return StreamSupport.stream(keysIter.spliterator(), false)
			.map(key -> key.substring(DEFAULT_KEY_PREFIX.length()))
			.collect(Collectors.toList());
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		RList<String> redisList = redissonClient.getList(DEFAULT_KEY_PREFIX + conversationId);
		return redisList.readAll()
			.parallelStream()
			.map(this::deserializeMessage)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");
		RList<String> redisList = redissonClient.getList(DEFAULT_KEY_PREFIX + conversationId);
		redisList.delete();
		List<String> serializedMessages = messages.stream().map(this::serializeMessage).toList();
		redisList.addAll(serializedMessages);
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		RList<String> redisList = redissonClient.getList(DEFAULT_KEY_PREFIX + conversationId);
		redisList.delete();
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
		RList<Object> list = redissonClient.getList(key);
		int size = list.size();
		if (size < maxLimit) {
			return;
		}
		list.trim(deleteSize, -1);
	}

	@Override
	public void close() {
		if (redissonClient != null && !redissonClient.isShutdown()) {
			try {
				int activeConnections = redissonClient.getConfig().getNettyThreads();
				logger.info("Shutting down Redisson with {} active connections", activeConnections);
				redissonClient.shutdown();
				logger.info("Redisson client shutdown completed");
			}
			catch (Exception e) {
				logger.error("Error shutting down Redisson client", e);
			}
		}
	}

}
