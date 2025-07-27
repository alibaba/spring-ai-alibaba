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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

@Service
public class DataSourceService {

	private static final Logger log = LoggerFactory.getLogger(DataSourceService.class);

	private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

	private final Map<String, String> dataSourceTypeMap = new ConcurrentHashMap<>();

	/**
	 * 添加数据源
	 */
	public void addDataSource(String name, String url, String username, String password, String driverClassName) {
		addDataSource(name, url, username, password, driverClassName, null);
	}

	/**
	 * 添加数据源（包含类型信息）
	 */
	public void addDataSource(String name, String url, String username, String password, String driverClassName,
			String type) {
		try {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setUrl(url);
			dataSource.setUsername(username);
			dataSource.setPassword(password);
			dataSource.setDriverClassName(driverClassName);
			dataSourceMap.put(name, dataSource);
			if (type != null) {
				dataSourceTypeMap.put(name, type);
			}
			log.info("Added datasource '{}' with url: {} and type: {}", name, url, type);
		}
		catch (Exception e) {
			log.error("Failed to add datasource '{}'", name, e);
		}
	}

	/**
	 * 获取默认数据源连接（使用第一个可用的数据源）
	 */
	public Connection getConnection() throws SQLException {
		java.util.Set<String> names = getDataSourceNames();
		if (names.isEmpty()) {
			throw new SQLException("No datasources available");
		}

		String defaultName = names.iterator().next();
		return getConnection(defaultName);
	}

	/**
	 * 通过名称获取连接
	 */
	public Connection getConnection(String datasourceName) throws SQLException {
		DataSource dataSource = getDataSource(datasourceName);
		if (dataSource == null) {
			throw new SQLException("DataSource '" + datasourceName + "' not found");
		}
		return dataSource.getConnection();
	}

	/**
	 * 获取默认数据源
	 */
	public DataSource getDataSource() {
		java.util.Set<String> names = getDataSourceNames();
		if (names.isEmpty()) {
			return null;
		}

		String defaultName = names.iterator().next();
		return getDataSource(defaultName);
	}

	/**
	 * 通过名称获取数据源
	 */
	public DataSource getDataSource(String datasourceName) {
		DataSource dataSource = dataSourceMap.get(datasourceName);
		if (dataSource == null) {
			log.warn("DataSource '{}' not found", datasourceName);
		}
		return dataSource;
	}

	/**
	 * 获取所有数据源名称
	 */
	public java.util.Set<String> getDataSourceNames() {
		return dataSourceMap.keySet();
	}

	/**
	 * 检查数据源是否存在
	 */
	public boolean hasDataSource(String datasourceName) {
		return dataSourceMap.containsKey(datasourceName);
	}

	/**
	 * 获取数据源数量
	 */
	public int getDataSourceCount() {
		return dataSourceMap.size();
	}

	/**
	 * 获取数据源类型
	 */
	public String getDataSourceType(String datasourceName) {
		return dataSourceTypeMap.get(datasourceName);
	}

	/**
	 * 获取所有数据源类型映射
	 */
	public Map<String, String> getDataSourceTypeMap() {
		return new ConcurrentHashMap<>(dataSourceTypeMap);
	}

	/**
	 * 获取默认数据源类型（使用第一个可用的数据源）
	 */
	public String getDataSourceType() {
		java.util.Set<String> names = getDataSourceNames();
		if (names.isEmpty()) {
			return null;
		}

		String defaultName = names.iterator().next();
		return getDataSourceType(defaultName);
	}

	/**
	 * Close resources if needed (no-op for DriverManagerDataSource)
	 */
	public void close() {
		log.info("Closing DataSourceService resources (no-op)");
	}

	/**
	 * 关闭所有数据源连接
	 */
	public void closeAllConnections() {
		log.info("Closing all datasource connections");
		// DriverManagerDataSource 会自动管理连接，这里主要是记录日志
		// 如果需要强制关闭连接池，可以在这里添加逻辑
	}

	/**
	 * 获取所有数据源信息（名称和类型）
	 */
	public Map<String, String> getAllDatasourceInfo() {
		return new ConcurrentHashMap<>(dataSourceTypeMap);
	}

}
