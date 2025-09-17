/*
 * Copyright 2023-2025 the original author or authors.
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
package com.alibaba.cloud.ai.autoconfigure.memory.redis;

import com.alibaba.cloud.ai.autoconfigure.memory.ChatMemoryAutoConfiguration;
import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Redis Chat Memory using Redisson.
 *
 * @author benym
 * @since 2025/7/30 19:01
 */
@AutoConfiguration(before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ RedissonRedisChatMemoryRepository.class, RedissonClient.class })
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
@ConditionalOnProperty(name = "spring.ai.memory.redis.client-type", havingValue = "redisson")
public class RedissonRedisChatMemoryConnectionAutoConfiguration
		extends RedisChatMemoryConnectionAutoConfiguration<RedissonRedisChatMemoryRepository> {

	private static final Logger logger = LoggerFactory
		.getLogger(RedissonRedisChatMemoryConnectionAutoConfiguration.class);

	public RedissonRedisChatMemoryConnectionAutoConfiguration(RedisChatMemoryProperties properties,
			RedisChatMemoryConnectionDetails connectionDetails, ObjectProvider<SslBundles> sslBundles) {
		super(properties, connectionDetails, sslBundles);
	}

	@Override
	@Bean
	@ConditionalOnMissingBean
	protected RedissonRedisChatMemoryRepository buildRedisChatMemoryRepository() {
		return super.buildRedisChatMemoryRepository();
	}

	@Override
	protected RedissonRedisChatMemoryRepository createStandaloneChatMemoryRepository(
			RedisChatMemoryStandaloneConfiguration standaloneConfiguration) {
		logger.info("Configuring Redis Standalone chat memory repository using Redisson");
		return RedissonRedisChatMemoryRepository.builder()
			.host(standaloneConfiguration.hostName())
			.port(standaloneConfiguration.port())
			.username(standaloneConfiguration.username())
			.password(standaloneConfiguration.password())
			.timeout(standaloneConfiguration.timeout())
			.sslBundles(standaloneConfiguration.sslBundles())
			.useSsl(standaloneConfiguration.ssl().isEnabled())
			.bundle(standaloneConfiguration.ssl().getBundle())
			.build();
	}

	@Override
	protected RedissonRedisChatMemoryRepository createClusterChatMemoryRepository(
			RedisChatMemoryClusterConfiguration clusterConfiguration) {
		logger.info("Configuring Redis Cluster chat memory repository using Redisson");
		return RedissonRedisChatMemoryRepository.builder()
			.nodes(clusterConfiguration.nodeAddresses())
			.username(clusterConfiguration.username())
			.password(clusterConfiguration.password())
			.timeout(clusterConfiguration.timeout())
			.sslBundles(clusterConfiguration.sslBundles())
			.useSsl(clusterConfiguration.ssl().isEnabled())
			.bundle(clusterConfiguration.ssl().getBundle())
			.build();
	}

}
