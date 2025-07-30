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

import com.alibaba.cloud.ai.memory.memcached.MemcachedChatMemoryRepository;
import com.alibaba.cloud.ai.toolcalling.memcached.MemcachedService;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Auto-configuration for Memcached chat memory repository. auth: dahua
 */
@AutoConfiguration(before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ MemcachedChatMemoryRepository.class, MemcachedClient.class })
@EnableConfigurationProperties(MemcachedChatMemoryProperties.class)
public class MemcachedChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MemcachedChatMemoryAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	MemcachedChatMemoryRepository memcachedChatMemoryRepository(MemcachedChatMemoryProperties properties)
			throws IOException {
		MemcachedClient memcachedClient = new MemcachedClient(
				new InetSocketAddress(properties.getHost(), properties.getPort()));
		logger.info("Configuring Memcached chat memory repository");
		return new MemcachedChatMemoryRepository(new MemcachedService(memcachedClient));
	}

}
