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

package com.alibaba.cloud.ai.mcp.router.core.discovery;

import com.alibaba.cloud.ai.mcp.router.config.DbMcpProperties;
import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DbMcpServiceDiscovery implements McpServiceDiscovery {

	private static final Logger log = LoggerFactory.getLogger(DbMcpServiceDiscovery.class);

	private final DataSource dataSource;

	private final String querySql;

	public DbMcpServiceDiscovery(DbMcpProperties properties) {
		this.dataSource = createDataSource(properties);
		this.querySql = buildQuerySql(properties);
	}

	@Override
	public McpServerInfo getService(String serviceName) {
		McpServerInfo serverInfo = null;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			connection = dataSource.getConnection();
			preparedStatement = connection.prepareStatement(querySql);
			preparedStatement.setString(1, serviceName);
			resultSet = preparedStatement.executeQuery();

			if (resultSet.next()) {
				serverInfo = mapResultSetToMcpServerInfo(resultSet);
			}
		}
		catch (SQLException e) {
			log.error("Failed to get service {} from database", serviceName, e);
		}
		finally {
			closeResources(resultSet, preparedStatement, connection);
		}

		return serverInfo;
	}

	private DataSource createDataSource(DbMcpProperties properties) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(properties.getUrl());
		config.setUsername(properties.getUsername());
		config.setPassword(properties.getPassword());
		config.setDriverClassName(properties.getDriverClassName());
		config.setMaximumPoolSize(properties.getMaxPoolSize());
		config.setMinimumIdle(properties.getMinIdle());
		config.setConnectionTimeout(properties.getConnectionTimeout());

		return new HikariDataSource(config);
	}

	private String buildQuerySql(DbMcpProperties properties) {
		if (StringUtils.hasText(properties.getQuerySql())) {
			return properties.getQuerySql();
		}

		// default query SQL, assuming table structure matches McpServerInfo fields
		return "SELECT name, description, protocol, version, endpoint, enabled, tags " + "FROM "
				+ properties.getTableName() + " " + "WHERE name = ? AND enabled = true";
	}

	private McpServerInfo mapResultSetToMcpServerInfo(ResultSet resultSet) throws SQLException {
		McpServerInfo serverInfo = new McpServerInfo();
		serverInfo.setName(resultSet.getString("name"));
		serverInfo.setDescription(resultSet.getString("description"));
		serverInfo.setProtocol(resultSet.getString("protocol"));
		serverInfo.setVersion(resultSet.getString("version"));
		serverInfo.setEndpoint(resultSet.getString("endpoint"));
		serverInfo.setEnabled(resultSet.getBoolean("enabled"));

		// parse tags, assuming split with ','
		String tagsStr = resultSet.getString("tags");
		if (StringUtils.hasText(tagsStr)) {
			List<String> tags = Arrays.asList(tagsStr.split(","));
			serverInfo.setTags(new ArrayList<>(tags));
		}

		return serverInfo;
	}

	private void closeResources(ResultSet resultSet, PreparedStatement statement, Connection connection) {
		closeQuietly(resultSet, "ResultSet");
		closeQuietly(statement, "PreparedStatement");
		closeQuietly(connection, "Connection");
	}

	private void closeQuietly(AutoCloseable resource, String name) {
		if (resource != null) {
			try {
				if (resource instanceof Connection conn) {
					if (!conn.isClosed()) {
						conn.close();
					}
				}
				else {
					resource.close();
				}
			}
			catch (Exception e) {
				log.error("Failed to close {}", name, e);
			}
		}
	}

}
