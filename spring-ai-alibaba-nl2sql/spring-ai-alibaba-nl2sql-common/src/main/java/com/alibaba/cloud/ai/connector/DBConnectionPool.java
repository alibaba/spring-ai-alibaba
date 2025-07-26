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
package com.alibaba.cloud.ai.connector;

import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.enums.ErrorCodeEnum;

import java.sql.Connection;

/**
 * DataAgent data connection pool, used to maintain the data source connection information
 * required by DataAgent
 */

public interface DBConnectionPool extends AutoCloseable {

	/**
	 * Ping the database to check if the connection is valid.
	 * @param config the database configuration
	 * @return ErrorCodeEnum indicating the result of the ping operation
	 */
	ErrorCodeEnum ping(DbConfig config);

	/**
	 * Get a database connection from the pool.
	 * @param config the database configuration
	 * @return a Connection object representing the database connection
	 */
	Connection getConnection(DbConfig config);

}
