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

import com.alibaba.cloud.ai.memory.jdbc.SQLiteChatMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Auto-configuration for SQLite chat memory repository.
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass({ SQLiteChatMemoryRepository.class, DataSource.class, JdbcTemplate.class })
@ConditionalOnProperty(prefix = "spring.ai.memory.sqlite", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@EnableConfigurationProperties(SQLiteChatMemoryProperties.class)
public class SQLiteChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(SQLiteChatMemoryAutoConfiguration.class);

	@Bean
	@Qualifier("sqliteChatMemoryRepository")
	@ConditionalOnMissingBean(name = "sqliteChatMemoryRepository")
	SQLiteChatMemoryRepository sqliteChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		logger.info("Configuring SQLite chat memory repository");
		return SQLiteChatMemoryRepository.sqliteBuilder().jdbcTemplate(jdbcTemplate).build();
	}

}
