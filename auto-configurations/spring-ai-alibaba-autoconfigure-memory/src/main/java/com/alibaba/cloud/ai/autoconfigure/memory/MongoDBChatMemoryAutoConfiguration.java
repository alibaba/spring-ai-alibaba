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

import com.alibaba.cloud.ai.memory.mongodb.MongoDBChatMemoryRepository;
import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MongoDB chat memory repository.
 */
@AutoConfiguration(before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ MongoDBChatMemoryRepository.class, MongoClient.class })
@EnableConfigurationProperties(MongoDBChatMemoryProperties.class)
public class MongoDBChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MongoDBChatMemoryAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	MongoDBChatMemoryRepository redisChatMemoryRepository(MongoDBChatMemoryProperties properties) {
		logger.info("Configuring MongoDB chat memory repository");
		return MongoDBChatMemoryRepository.builder()
			.host(properties.getHost())
			.port(properties.getPort())
			.authDatabaseName(properties.getAuthDatabaseName())
			.databaseName(properties.getDatabaseName())
			.userName(properties.getUserName())
			.password(properties.getPassword())
			.build();
	}

}
