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

import com.alibaba.cloud.ai.memory.tablestore.TablestoreChatMemoryRepository;
import com.alicloud.openservices.tablestore.SyncClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

/**
 * Auto-configuration for Tablestore chat memory repository.
 */
@ConditionalOnClass({ TablestoreChatMemoryRepository.class, SyncClient.class })
@EnableConfigurationProperties(TablestoreChatMemoryProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.memory.tablestore", name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class TablestoreChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TablestoreChatMemoryAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	SyncClient tablestoreSyncClient(TablestoreChatMemoryProperties properties) {
		return new SyncClient(properties.getEndpoint(), properties.getAccessKeyId(), properties.getAccessKeySecret(),
				properties.getInstanceName());
	}

	@Bean
	@ConditionalOnMissingBean
	TablestoreChatMemoryRepository tablestoreChatMemoryRepository(SyncClient syncClient,
			TablestoreChatMemoryProperties properties) {
		logger.info("Configuring Tablestore chat memory repository");
		return new TablestoreChatMemoryRepository(syncClient, properties.getSessionTableName(),
				properties.getSessionSecondaryIndexName(), Collections.emptyList(), properties.getMessageTableName(),
				properties.getMessageSecondaryIndexName());
	}

}
