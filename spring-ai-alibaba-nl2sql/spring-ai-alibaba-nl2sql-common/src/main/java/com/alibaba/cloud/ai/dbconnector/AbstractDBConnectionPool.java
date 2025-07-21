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

package com.alibaba.cloud.ai.dbconnector;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractDBConnectionPool implements DBConnectionPool {

	private static final Logger log = LoggerFactory.getLogger(AbstractDBConnectionPool.class);

	/**
	 * DataSource cache to ensure that each configuration creates DataSource only once.
	 */
	private static final ConcurrentHashMap<String, DataSource> DATA_SOURCE_CACHE = new ConcurrentHashMap<>();

	/**
	 * 方言
	 */
	public abstract DatabaseDialectEnum getDialect();

	/**
	 * 驱动
	 */
	public abstract String getDriver();

	/**
	 * 错误信息映射
	 */
	public abstract ErrorCodeEnum errorMapping(String sqlState);

	public ErrorCodeEnum ping(DbConfig config) {
		String jdbcUrl = config.getUrl();
		try (Connection connection = DriverManager.getConnection(jdbcUrl, config.getUsername(), config.getPassword());
				Statement stmt = connection.createStatement();) {
			if (BizDataSourceTypeEnum.isPgDialect(config.getConnectionType())) {
				String sql = "SELECT count(*) FROM information_schema.schemata WHERE schema_name = '%s'";
				ResultSet rs = stmt.executeQuery(String.format(sql, config.getSchema()));
				if (rs.next()) {
					int count = rs.getInt(1);
					rs.close();
					if (count == 0) {
						log.info("the specified schema '{}' does not exist.", config.getSchema());
						return ErrorCodeEnum.SCHEMA_NOT_EXIST_3D070;
					}
				}
				rs.close();
			}
			return ErrorCodeEnum.SUCCESS;
		}
		catch (SQLException e) {
			log.error("test db connection error, url:{}, state:{}, message:{}", jdbcUrl, e.getSQLState(),
					e.getMessage());
			return errorMapping(e.getSQLState());
		}
	}

	public Connection getConnection(DbConfig config) {

		// Test the connection before returning it
		ErrorCodeEnum pingResult = this.ping(config);
		if (pingResult != ErrorCodeEnum.SUCCESS) {
			throw new RuntimeException("Database connection test failed: " + pingResult);
		}
		String jdbcUrl = config.getUrl();

		try {
			// Generate cache key based on connection parameters
			String cacheKey = generateCacheKey(jdbcUrl, config.getUsername(), config.getPassword());

			// Use computeIfAbsent to ensure thread safety and avoid duplicate DataSource
			// creation
			DataSource dataSource = DATA_SOURCE_CACHE.computeIfAbsent(cacheKey, key -> {
				try {
					log.debug("Creating new DataSource for key: {}", key);
					return createdDataSource(jdbcUrl, config.getUsername(), config.getPassword());
				}
				catch (Exception e) {
					log.error("Failed to create DataSource for key: {}", key, e);
					throw new RuntimeException("Failed to create DataSource", e);
				}
			});

			return dataSource.getConnection();
		}
		catch (SQLException e) {
			log.error("create db connection error, e:" + e);
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generate cache key based on connection parameters.
	 * @param url the database URL
	 * @param username the database username
	 * @param password the database password
	 * @return the cache key
	 */
	private String generateCacheKey(String url, String username, String password) {
		return url + "|" + username + "|" + Objects.hashCode(password);
	}

	/**
	 * Clear DataSource cache and close all cached DataSource instances. This method is
	 * useful for resource cleanup in special scenarios.
	 */
	public static void clearDataSourceCache() {
		DATA_SOURCE_CACHE.values().forEach(dataSource -> {
			if (dataSource instanceof DruidDataSource) {
				((DruidDataSource) dataSource).close();
			}
		});
		DATA_SOURCE_CACHE.clear();
		log.info("DataSource cache cleared");
	}

	public DataSource createdDataSource(String url, String username, String password) throws Exception {

		DruidDataSource dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(
				Map.of(DruidDataSourceFactory.PROP_DRIVERCLASSNAME, getDriver(), DruidDataSourceFactory.PROP_URL, url,
						DruidDataSourceFactory.PROP_USERNAME, username, DruidDataSourceFactory.PROP_PASSWORD, password,
						DruidDataSourceFactory.PROP_INITIALSIZE, "1", DruidDataSourceFactory.PROP_MINIDLE, "1",
						DruidDataSourceFactory.PROP_MAXACTIVE, "3", DruidDataSourceFactory.PROP_MAXWAIT, "6000",
						DruidDataSourceFactory.PROP_TIMEBETWEENEVICTIONRUNSMILLIS, "60000",
						DruidDataSourceFactory.PROP_FILTERS, "wall,stat"));
		dataSource.setBreakAfterAcquireFailure(Boolean.TRUE);
		dataSource.setConnectionErrorRetryAttempts(2);

		return dataSource;
	}

}
