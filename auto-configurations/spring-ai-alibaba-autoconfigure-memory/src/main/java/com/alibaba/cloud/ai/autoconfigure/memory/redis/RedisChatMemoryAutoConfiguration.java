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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Redis Memory support.
 *
 * @author benym
 * @since 2025/7/30 23:35
 */
@AutoConfiguration
@EnableConfigurationProperties(RedisChatMemoryProperties.class)
@Import({ JedisRedisChatMemoryConnectionAutoConfiguration.class,
		LettuceRedisChatMemoryConnectionAutoConfiguration.class,
		RedissonRedisChatMemoryConnectionAutoConfiguration.class })
public class RedisChatMemoryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(RedisMemoryConnectionDetails.class)
	RedisChatMemoryConnectionDetails redisChatMemoryConnectionDetails(RedisChatMemoryProperties properties) {
		return new RedisChatMemoryConnectionDetails(properties);
	}

}
