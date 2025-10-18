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
package com.alibaba.cloud.ai.autoconfigure.redis.memory;

import com.alibaba.cloud.ai.memory.RedisChatMemoryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Libres-coder
 */
@AutoConfiguration
@ConditionalOnClass({ JedisPool.class, RedisChatMemoryRepository.class })
@ConditionalOnProperty(prefix = RedisChatMemoryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
public class RedisChatMemoryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public RedisChatMemoryRepository redisChatMemoryRepository(RedisChatMemoryProperties properties) {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(properties.getMaxTotal());
		poolConfig.setMaxIdle(properties.getMaxIdle());
		poolConfig.setMinIdle(properties.getMinIdle());

		return new RedisChatMemoryRepository.Builder().host(properties.getHost())
			.port(properties.getPort())
			.database(properties.getDatabase())
			.password(properties.getPassword())
			.timeout(properties.getTimeout())
			.poolConfig(poolConfig)
			.build();
	}

}

