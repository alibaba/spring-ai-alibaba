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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MySQL chat memory repository.
 */
@ConfigurationProperties(MysqlChatMemoryProperties.CONFIG_PREFIX)
public class MysqlChatMemoryProperties {

	public static final String CONFIG_PREFIX = "spring.ai.chat.memory.repository.jdbc.mysql";

	private boolean initializeSchema = true;

	public boolean isInitializeSchema() {
		return this.initializeSchema;
	}

	public void setInitializeSchema(boolean initializeSchema) {
		this.initializeSchema = initializeSchema;
	}

	/**
	 * JDBC URL of the database.
	 */
	private String jdbcUrl;

	/**
	 * Database username.
	 */
	private String username;

	/**
	 * Database password.
	 */
	private String password;

	/**
	 * Fully qualified name of the JDBC driver class.
	 */
	private String driverClassName = "com.mysql.cj.jdbc.Driver";

	/**
	 * Whether to enable custom datasource configuration.
	 */
	private boolean enabled = false;

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
