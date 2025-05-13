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
package com.alibaba.cloud.ai.reader.sqlite;

import java.util.List;

/**
 * Configuration class for SQLite document reader Contains connection information and
 * query settings
 *
 * @author jens papenhagen
 **/
public class SQLiteResource {

	// SQLite connection properties
	private final String host; // SQLite server hostname or IP address

	private final int port; // SQLite server port number, default is 3306

	private final String database; // Name of the database to connect to

	private final String username; // SQLite user name for authentication

	private final String password; // SQLite password for authentication

	// Query settings
	private final String query; // SQL query to execute

	private final List<String> contentColumns; // Columns to include in document content

	private final List<String> metadataColumns; // Columns to include in document metadata

	public static final String SOURCE = "SQLite";

	// Default values for SQLite connection
	public static final String DEFAULT_HOST = "127.0.0.1";

	public static final int DEFAULT_PORT = 3306;

	public static final String DEFAULT_USERNAME = "root";

	public static final String DEFAULT_PASSWORD = "root";

	public SQLiteResource(String host, int port, String database, String username, String password, String query,
			List<String> contentColumns, List<String> metadataColumns) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		this.query = query;
		this.contentColumns = contentColumns;
		this.metadataColumns = metadataColumns;
	}

	/**
	 * Constructor with default host and port
	 * @param database Database name
	 * @param username SQLite username
	 * @param password SQLite password
	 * @param query SQL query to execute
	 * @param contentColumns Columns to include in document content
	 * @param metadataColumns Columns to include in document metadata
	 */
	public SQLiteResource(String database, String username, String password, String query, List<String> contentColumns,
			List<String> metadataColumns) {
		this(DEFAULT_HOST, DEFAULT_PORT, database, username, password, query, contentColumns, metadataColumns);
	}

	/**
	 * Constructor with all default connection parameters
	 * @param database Database name
	 * @param query SQL query to execute
	 * @param contentColumns Columns to include in document content
	 * @param metadataColumns Columns to include in document metadata
	 */
	public SQLiteResource(String database, String query, List<String> contentColumns, List<String> metadataColumns) {
		this(DEFAULT_HOST, DEFAULT_PORT, database, DEFAULT_USERNAME, DEFAULT_PASSWORD, query, contentColumns,
				metadataColumns);
	}

	// Getters
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getDatabase() {
		return database;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getQuery() {
		return query;
	}

	public List<String> getTextColumns() {
		return contentColumns;
	}

	public List<String> getMetadataColumns() {
		return metadataColumns;
	}

	/**
	 * Get JDBC URL for SQLite connection
	 * @return JDBC URL string
	 */
	public String getJdbcUrl() {
		return String.format("jdbc:sqlite://%s:%d/%s", host, port, database);
	}

}
