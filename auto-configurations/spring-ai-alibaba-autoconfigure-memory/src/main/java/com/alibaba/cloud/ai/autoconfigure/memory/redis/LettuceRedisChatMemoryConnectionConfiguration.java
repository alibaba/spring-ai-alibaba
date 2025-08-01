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
import com.alibaba.cloud.ai.memory.redis.LettuceRedisChatMemoryRepository;
import io.lettuce.core.RedisClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Redis Chat Memory using Lettuce.
 *
 * @author benym
 * @date 2025/7/31 16:23
 */
@AutoConfiguration(before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ LettuceRedisChatMemoryRepository.class, RedisClient.class })
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
@ConditionalOnProperty(name = "spring.ai.memory.redis.client-type", havingValue = "lettuce", matchIfMissing = true)
public class LettuceRedisChatMemoryConnectionConfiguration extends RedisMemoryConnectionConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(LettuceRedisChatMemoryConnectionConfiguration.class);

	public LettuceRedisChatMemoryConnectionConfiguration(RedisChatMemoryProperties properties,
			RedisChatMemoryConnectionDetails connectionDetails) {
		super(properties, connectionDetails);
	}

	@Bean
	@ConditionalOnMissingBean
	LettuceRedisChatMemoryRepository redisChatMemoryRepository() {
		if (getClusterConfiguration() != null) {
			logger.info("Configuring Redis Cluster chat memory repository using Lettuce");
			RedisMemoryClusterConfiguration clusterConfiguration = getClusterConfiguration();
			return LettuceRedisChatMemoryRepository.builder()
				.nodes(clusterConfiguration.nodeAddresses())
				.username(clusterConfiguration.username())
				.password(clusterConfiguration.password())
				.timeout(clusterConfiguration.timeout())
				.build();
		}
		logger.info("Configuring Redis Standalone chat memory repository using Lettuce");
		RedisMemoryStandaloneConfiguration standaloneConfiguration = getStandaloneConfiguration();
		return LettuceRedisChatMemoryRepository.builder()
			.host(standaloneConfiguration.hostName())
			.port(standaloneConfiguration.port())
			.username(standaloneConfiguration.username())
			.password(standaloneConfiguration.password())
			.timeout(standaloneConfiguration.timeout())
			.build();
	}

}
