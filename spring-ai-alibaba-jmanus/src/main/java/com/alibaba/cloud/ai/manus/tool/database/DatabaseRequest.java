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
package com.alibaba.cloud.ai.manus.tool.database;

/**
 * Database tool request object for encapsulating database operation request parameters
 *
 * <p>
 * This object contains all parameters required for database operations, supporting
 * multiple database operation types, including SQL execution, table information queries,
 * index queries, etc.
 * </p>
 *
 * @author Spring AI Alibaba Team
 * @since 1.0.0
 */
public class DatabaseRequest {

	/**
	 * Database operation type
	 *
	 * <p>
	 * Supported operation types include:
	 * </p>
	 * <ul>
	 * <li>{@code execute_sql} - Execute SQL queries</li>
	 * <li>{@code get_table_name} - Get table name list</li>
	 * <li>{@code get_table_meta} - Get table metadata information</li>
	 * <li>{@code get_table_index} - Get table index information</li>
	 * <li>{@code get_datasource_info} - Get data source information</li>
	 * </ul>
	 */
	private String action;

	/**
	 * SQL query statement
	 *
	 * <p>
	 * Used when operation type is {@code execute_sql}. Contains the SQL query statement
	 * to execute.
	 * </p>
	 */
	private String query;

	/**
	 * Text parameter
	 *
	 * <p>
	 * Used to specify table names, comments or other text information. Used for filtering
	 * when operation type is {@code get_table_name}, {@code get_table_meta},
	 * {@code get_table_index}.
	 * </p>
	 */
	private String text;

	/**
	 * Data source name
	 *
	 * <p>
	 * Specifies the data source name to use. If empty or not specified, the default data
	 * source will be used. Supports data source switching in multi-data source
	 * environments.
	 * </p>
	 */
	private String datasourceName;

	/**
	 * Get database operation type
	 * @return Operation type string, such as "execute_sql", "get_table_name", etc.
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Set database operation type
	 * @param action Operation type string, cannot be null
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * Get SQL query statement
	 * @return SQL query statement, may be null
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Set SQL query statement
	 * @param query SQL query statement, used when operation type is "execute_sql"
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Get text parameter
	 * @return Text parameter for table name filtering etc., may be null
	 */
	public String getText() {
		return text;
	}

	/**
	 * Set text parameter
	 * @param text Text parameter for specifying table names, comments or other filter
	 * conditions
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Get data source name
	 * @return Data source name, if null or empty then use default data source
	 */
	public String getDatasourceName() {
		return datasourceName;
	}

	/**
	 * Set data source name
	 * @param datasourceName Data source name, used to specify the data source to use
	 */
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

}
