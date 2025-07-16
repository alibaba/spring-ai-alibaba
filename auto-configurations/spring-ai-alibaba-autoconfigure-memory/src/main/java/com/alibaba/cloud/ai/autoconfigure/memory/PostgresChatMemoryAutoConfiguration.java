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

import com.alibaba.cloud.ai.memory.jdbc.PostgresChatMemoryRepository;
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
 * Auto-configuration for PostgreSQL chat memory repository.
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass({ PostgresChatMemoryRepository.class, DataSource.class, JdbcTemplate.class })
@ConditionalOnProperty(prefix = "spring.ai.memory.postgres", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@EnableConfigurationProperties(PostgresChatMemoryProperties.class)
public class PostgresChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(PostgresChatMemoryAutoConfiguration.class);

	@Bean
	@Qualifier("postgresChatMemoryRepository")
	@ConditionalOnMissingBean(name = "postgresChatMemoryRepository")
	PostgresChatMemoryRepository postgresChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		logger.info("Configuring PostgreSQL chat memory repository");
		return PostgresChatMemoryRepository.postgresBuilder().jdbcTemplate(jdbcTemplate).build();
	}

}
