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

import org.springframework.stereotype.Service;

import static com.alibaba.cloud.ai.dbconnector.ErrorCodeEnum.*;

@Service("postgrSqlJdbcConnectionPool")
public class PostgreSqlJdbcConnectionPool extends AbstractDBConnectionPool {

	@Override
	public DatabaseDialectEnum getDialect() {
		return DatabaseDialectEnum.POSTGRESQL;
	}

	@Override
	public String getDriver() {
		return "org.postgresql.Driver";
	}

	@Override
	public ErrorCodeEnum errorMapping(String sqlState) {
		ErrorCodeEnum ret = ErrorCodeEnum.fromCode(sqlState);
		if (ret != null) {
			return ret;
		}
		return switch (sqlState) {
			case "08S01" -> DATASOURCE_CONNECTION_FAILURE_08S01;
			case "28000" -> PASSWORD_ERROR_28000;
			case "42000" -> DATABASE_NOT_EXIST_42000;
			default -> OTHERS;
		};
	}

}
