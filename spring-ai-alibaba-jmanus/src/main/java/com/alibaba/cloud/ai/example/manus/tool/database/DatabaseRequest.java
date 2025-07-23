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
 * 数据库工具请求对象，用于封装数据库操作请求参数
 *
 * <p>
 * 该对象包含执行数据库操作所需的所有参数，支持多种数据库操作类型， 包括SQL执行、表信息查询、索引查询等。
 * </p>
 *
 * @author Spring AI Alibaba Team
 * @since 1.0.0
 */
public class DatabaseRequest {

	/**
	 * 数据库操作类型
	 *
	 * <p>
	 * 支持的操作类型包括：
	 * </p>
	 * <ul>
	 * <li>{@code execute_sql} - 执行SQL查询</li>
	 * <li>{@code get_table_name} - 获取表名列表</li>
	 * <li>{@code get_table_meta} - 获取表元数据信息</li>
	 * <li>{@code get_table_index} - 获取表索引信息</li>
	 * <li>{@code get_datasource_info} - 获取数据源信息</li>
	 * </ul>
	 */
	private String action;

	/**
	 * SQL查询语句
	 *
	 * <p>
	 * 当操作类型为 {@code execute_sql} 时使用此字段。 包含要执行的SQL查询语句。
	 * </p>
	 */
	private String query;

	/**
	 * 文本参数
	 *
	 * <p>
	 * 用于指定表名、注释或其他文本信息。 当操作类型为 {@code get_table_name}、{@code get_table_meta}、
	 * {@code get_table_index} 时使用此字段进行过滤。
	 * </p>
	 */
	private String text;

	/**
	 * 数据源名称
	 *
	 * <p>
	 * 指定要使用的数据源名称。如果为空或未指定，则使用默认数据源。 支持多数据源环境下的数据源切换。
	 * </p>
	 */
	private String datasourceName;

	/**
	 * 获取数据库操作类型
	 * @return 操作类型字符串，如 "execute_sql"、"get_table_name" 等
	 */
	public String getAction() {
		return action;
	}

	/**
	 * 设置数据库操作类型
	 * @param action 操作类型字符串，不能为null
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * 获取SQL查询语句
	 * @return SQL查询语句，可能为null
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * 设置SQL查询语句
	 * @param query SQL查询语句，当操作类型为 "execute_sql" 时使用
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * 获取文本参数
	 * @return 文本参数，用于表名过滤等，可能为null
	 */
	public String getText() {
		return text;
	}

	/**
	 * 设置文本参数
	 * @param text 文本参数，用于指定表名、注释或其他过滤条件
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * 获取数据源名称
	 * @return 数据源名称，如果为null或空则使用默认数据源
	 */
	public String getDatasourceName() {
		return datasourceName;
	}

	/**
	 * 设置数据源名称
	 * @param datasourceName 数据源名称，用于指定要使用的数据源
	 */
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

}
