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

package com.alibaba.cloud.ai.autoconfigure.memory;

import com.alibaba.cloud.ai.memory.redis.RedisChatMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.Jedis;

/**
 * Auto-configuration for Redis chat memory repository.
 */
@AutoConfiguration(before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ RedisChatMemoryRepository.class, Jedis.class })
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
public class RedisChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(RedisChatMemoryAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	RedisChatMemoryRepository redisChatMemoryRepository(RedisChatMemoryProperties properties) {
		logger.info("Configuring Redis chat memory repository");
		return RedisChatMemoryRepository.builder()
			.host(properties.getHost())
			.port(properties.getPort())
			.password(properties.getPassword())
			.timeout(properties.getTimeout())
			.build();
	}

}
