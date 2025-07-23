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
 * Configuration properties for mongodb chat memory.
 */
@ConfigurationProperties(prefix = "spring.ai.memory.mongodb")
public class MongoDBChatMemoryProperties {

	/**
	 * mongodb host URL
	 */
	private String host = "127.0.0.1";

	/**
	 * mongodb port.
	 */
	private int port = 27017;

	/**
	 * mongodb userName.
	 */
	private String userName;

	/**
	 * mongodb password.
	 */
	private String password;

	/**
	 * mongodb authDatabaseName.
	 */
	private String authDatabaseName = "admin";

	/**
	 * mongodb databaseName.
	 */
	private String databaseName = "spring_ai";

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthDatabaseName() {
		return authDatabaseName;
	}

	public void setAuthDatabaseName(String authDatabaseName) {
		this.authDatabaseName = authDatabaseName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

}
