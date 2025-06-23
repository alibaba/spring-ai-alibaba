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

import com.alibaba.cloud.ai.memory.jdbc.OracleChatMemoryRepository;
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
 * Auto-configuration for Oracle chat memory repository.
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnClass({ OracleChatMemoryRepository.class, DataSource.class, JdbcTemplate.class })
@ConditionalOnProperty(prefix = "spring.ai.memory.oracle", name = "enabled", havingValue = "true",
		matchIfMissing = false)
@EnableConfigurationProperties(OracleChatMemoryProperties.class)
public class OracleChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(OracleChatMemoryAutoConfiguration.class);

	@Bean
	@Qualifier("oracleChatMemoryRepository")
	@ConditionalOnMissingBean(name = "oracleChatMemoryRepository")
	OracleChatMemoryRepository oracleChatMemoryRepository(JdbcTemplate jdbcTemplate) {
		logger.info("Configuring Oracle chat memory repository");
		return OracleChatMemoryRepository.oracleBuilder().jdbcTemplate(jdbcTemplate).build();
	}

}
