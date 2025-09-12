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
	 * Add data source
	 */
	public void addDataSource(String name, String url, String username, String password, String driverClassName) {
		addDataSource(name, url, username, password, driverClassName, null);
	}

	/**
	 * Add data source (with type information)
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
	 * Get default data source connection (use first available data source)
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
	 * Get connection by name
	 */
	public Connection getConnection(String datasourceName) throws SQLException {
		DataSource dataSource = getDataSource(datasourceName);
		if (dataSource == null) {
			throw new SQLException("DataSource '" + datasourceName + "' not found");
		}
		return dataSource.getConnection();
	}

	/**
	 * Get default data source
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
	 * Get data source by name
	 */
	public DataSource getDataSource(String datasourceName) {
		DataSource dataSource = dataSourceMap.get(datasourceName);
		if (dataSource == null) {
			log.warn("DataSource '{}' not found", datasourceName);
		}
		return dataSource;
	}

	/**
	 * Get all data source names
	 */
	public java.util.Set<String> getDataSourceNames() {
		return dataSourceMap.keySet();
	}

	/**
	 * Check if data source exists
	 */
	public boolean hasDataSource(String datasourceName) {
		return dataSourceMap.containsKey(datasourceName);
	}

	/**
	 * Get data source count
	 */
	public int getDataSourceCount() {
		return dataSourceMap.size();
	}

	/**
	 * Get data source type
	 */
	public String getDataSourceType(String datasourceName) {
		return dataSourceTypeMap.get(datasourceName);
	}

	/**
	 * Get all data source type mappings
	 */
	public Map<String, String> getDataSourceTypeMap() {
		return new ConcurrentHashMap<>(dataSourceTypeMap);
	}

	/**
	 * Get default data source type (use first available data source)
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
	 * Close all data source connections
	 */
	public void closeAllConnections() {
		log.info("Closing all datasource connections");
		// DriverManagerDataSource automatically manages connections, mainly for logging
		// here
		// If need to force close connection pool, can add logic here
	}

	/**
	 * Get all data source information (name and type)
	 */
	public Map<String, String> getAllDatasourceInfo() {
		return new ConcurrentHashMap<>(dataSourceTypeMap);
	}

}
