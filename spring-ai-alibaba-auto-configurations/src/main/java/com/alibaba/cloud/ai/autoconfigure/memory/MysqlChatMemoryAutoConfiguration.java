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

import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Auto-configuration for MySQL chat memory repository.
 */
@AutoConfiguration(after = JdbcTemplateAutoConfiguration.class, before = ChatMemoryAutoConfiguration.class)
@ConditionalOnClass({ MysqlChatMemoryRepository.class, DataSource.class, JdbcTemplate.class })
@EnableConfigurationProperties(MysqlChatMemoryProperties.class)
public class MysqlChatMemoryAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MysqlChatMemoryAutoConfiguration.class);

	private final MysqlChatMemoryProperties properties;

	public MysqlChatMemoryAutoConfiguration(MysqlChatMemoryProperties properties) {
		this.properties = properties;
	}

	/**
	 * Creates a custom DataSource for MySQL chat memory if enabled in configuration.
	 * @return a DataSource configured with MySQL chat memory properties
	 */
	@Bean
	@ConditionalOnProperty(prefix = MysqlChatMemoryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(name = "mysqlChatMemoryDataSource")
	public DataSource mysqlChatMemoryDataSource() {
		logger.info("Creating custom DataSource for MySQL chat memory");
		return DataSourceBuilder.create()
			.url(properties.getJdbcUrl())
			.username(properties.getUsername())
			.password(properties.getPassword())
			.driverClassName(properties.getDriverClassName())
			.build();
	}

	/**
	 * Creates a custom JdbcTemplate for MySQL chat memory if custom DataSource is
	 * enabled.
	 * @param mysqlChatMemoryDataSource the custom DataSource for MySQL chat memory
	 * @return a JdbcTemplate configured with the custom DataSource
	 */
	@Bean
	@ConditionalOnProperty(prefix = MysqlChatMemoryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
	@ConditionalOnMissingBean(name = "mysqlChatMemoryJdbcTemplate")
	public JdbcTemplate mysqlChatMemoryJdbcTemplate(
			@Qualifier("mysqlChatMemoryDataSource") DataSource mysqlChatMemoryDataSource) {
		logger.info("Creating custom JdbcTemplate for MySQL chat memory");
		return new JdbcTemplate(mysqlChatMemoryDataSource);
	}

	/**
	 * Creates a MySQL chat memory repository with the appropriate JdbcTemplate. If a
	 * custom DataSource is configured, uses the custom JdbcTemplate; otherwise, falls
	 * back to the application's default JdbcTemplate.
	 */
	@Bean
	@ConditionalOnMissingBean
	public ChatMemoryRepository mysqlChatMemoryRepository(
			@Qualifier("mysqlChatMemoryJdbcTemplate") JdbcTemplate jdbcTemplate) {
		if (properties.isEnabled()) {
			logger.info("Configuring MySQL chat memory repository with custom DataSource");
			JdbcTemplate customTemplate = mysqlChatMemoryJdbcTemplate(mysqlChatMemoryDataSource());
			return MysqlChatMemoryRepository.mysqlBuilder().jdbcTemplate(customTemplate).build();
		}

		logger.info("Configuring MySQL chat memory repository with default DataSource");
		return MysqlChatMemoryRepository.mysqlBuilder().jdbcTemplate(jdbcTemplate).build();
	}

}