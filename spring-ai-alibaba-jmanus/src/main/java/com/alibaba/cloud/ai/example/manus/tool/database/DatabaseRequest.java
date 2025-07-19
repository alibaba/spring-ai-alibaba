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
package com.alibaba.cloud.ai.example.manus.tool.database;

/**
 * Database tool request object for encapsulating database operation request parameters
 */
public class DatabaseRequest {

	/**
	 * Database operation type. E.g. execute_sql, get_table_name, get_table_desc,
	 * get_table_index, get_table_lock, get_db_health_running, get_db_health_index_usage,
	 * get_chinese_initials
	 */
	private String action;

	/**
	 * SQL query or text input, depending on the action
	 */
	private String query;

	/**
	 * Text parameter, e.g. for table name, comment, or Chinese text
	 */
	private String text;

	/**
	 * Data source name. If not empty, use the specified datasource; otherwise use default
	 */
	private String datasourceName;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

}