/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.autoconfigure.mcp.router;

import com.alibaba.cloud.ai.mcp.router.config.McpSessionProperties;
import com.alibaba.cloud.ai.mcp.router.session.InMemoryMcpSessionStore;
import com.alibaba.cloud.ai.mcp.router.session.McpSessionStore;
import com.alibaba.cloud.ai.mcp.router.session.RedisMcpSessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * MCP Session Store auto-configuration
 *
 * @author Libres-coder
 * @since 2025.10.16
 */
@AutoConfiguration
@EnableConfigurationProperties(McpSessionProperties.class)
public class McpSessionStoreAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(McpSessionStoreAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(McpSessionStore.class)
	@ConditionalOnProperty(prefix = "spring.ai.alibaba.mcp.router.session", name = "store-type",
			havingValue = "memory", matchIfMissing = true)
	public McpSessionStore inMemoryMcpSessionStore() {
		logger.info("Initializing InMemoryMcpSessionStore");
		return new InMemoryMcpSessionStore();
	}

	@Bean
	@ConditionalOnMissingBean(McpSessionStore.class)
	@ConditionalOnClass(StringRedisTemplate.class)
	@ConditionalOnProperty(prefix = "spring.ai.alibaba.mcp.router.session", name = "store-type", havingValue = "redis")
	public McpSessionStore redisMcpSessionStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
			McpSessionProperties sessionProperties) {

		McpSessionProperties.Redis redisConfig = sessionProperties.getRedis();
		logger.info("Initializing RedisMcpSessionStore - keyPrefix: {}, ttl: {}s", redisConfig.getKeyPrefix(),
				redisConfig.getTtl());

		return new RedisMcpSessionStore(stringRedisTemplate, objectMapper, redisConfig.getKeyPrefix(),
				redisConfig.getTtl());
	}

}

