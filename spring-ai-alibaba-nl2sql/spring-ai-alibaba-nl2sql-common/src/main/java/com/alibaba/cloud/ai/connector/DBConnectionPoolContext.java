/**
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.connector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB connection pool context
 */
@Component
public class DBConnectionPoolContext {

	private final Map<String, DBConnectionPool> poolMap = new HashMap<>();

	@Autowired
	public DBConnectionPoolContext(List<DBConnectionPool> pools) {
		for (DBConnectionPool pool : pools) {
			// Get bean name from @Service("xxx") annotation on class
			String beanName = pool.getClass().getAnnotation(Service.class).value();
			poolMap.put(beanName, pool);
		}
	}

	/**
	 * Get corresponding DB connection pool based on database type
	 * @param type database type
	 * @return DB connection pool
	 */
	public DBConnectionPool getPoolByType(String type) {
		if (type == null || type.trim().isEmpty()) {
			return null;
		}
		return switch (type.toLowerCase()) {
			case "mysql", "mysqljdbcconnectionpool" -> poolMap.get("mysqlJdbcConnectionPool");
			case "postgresql", "postgres", "postgresqljdbcconnectionpool" ->
				poolMap.get("postgreSqlJdbcConnectionPool");
			default -> null;
		};
	}

}
