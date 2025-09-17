/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.alibaba.cloud.ai.manus.memory.repository.H2ChatMemoryRepository;
import com.alibaba.cloud.ai.manus.memory.repository.MysqlChatMemoryRepository;
import com.alibaba.cloud.ai.manus.memory.repository.PostgresChatMemoryRepository;

/**
 * @author dahua
 * @time 2025/8/5
 * @desc memory config for manus
 */
@Configuration
public class MemoryConfig {

	// import memory auto configuration
	// jmanus only support memory for mysql and postgresql now
	@Value("${spring.ai.memory.mysql.enabled:false}")
	private boolean mysqlEnabled;

	@Value("${spring.ai.memory.postgres.enabled:false}")
	private boolean postgresEnabled;

	@Value("${spring.ai.memory.h2.enabled:false}")
	private boolean h2Enabled;

	@Bean
	public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
		return MessageWindowChatMemory.builder().chatMemoryRepository(chatMemoryRepository).build();
	}

	@Bean
	public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
		ChatMemoryRepository chatMemoryRepository = null;
		if (mysqlEnabled) {
			chatMemoryRepository = MysqlChatMemoryRepository.mysqlBuilder().jdbcTemplate(jdbcTemplate).build();
		}
		else if (postgresEnabled) {
			chatMemoryRepository = PostgresChatMemoryRepository.postgresBuilder().jdbcTemplate(jdbcTemplate).build();
		}
		else if (h2Enabled) {
			chatMemoryRepository = H2ChatMemoryRepository.h2Builder().jdbcTemplate(jdbcTemplate).build();
		}
		if (chatMemoryRepository == null) {
			throw new RuntimeException("Please enable mysql or postgres or h2 memory");
		}
		return chatMemoryRepository;
	}

}
