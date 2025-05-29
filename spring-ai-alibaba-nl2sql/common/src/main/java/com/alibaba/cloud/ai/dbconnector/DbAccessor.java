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

import com.alibaba.cloud.ai.dbconnector.bo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.List;

@Service
public class DbAccessor {

	@Autowired
	private MysqlJdbcConnectionPool mysqlJdbcConnectionPool;

	@Autowired
	private PostgreSqlJdbcConnectionPool postgreSqlJdbcConnectionPool;

	@Autowired
	private DdlFactory ddlFactory;

	public DbAccessor(MysqlJdbcConnectionPool mysqlJdbcConnectionPool,
			PostgreSqlJdbcConnectionPool postgreSqlJdbcConnectionPool) {
		this.mysqlJdbcConnectionPool = mysqlJdbcConnectionPool;
		this.postgreSqlJdbcConnectionPool = postgreSqlJdbcConnectionPool;
	}

	private DBConnectionPool getConnectionPoolByType(DbConfig config) {
		if (config.getConnectionType().equals(DbAccessTypeEnum.JDBC.getCode())) {
			if (BizDataSourceTypeEnum.isMysqlDialect(config.getDialectType())) {
				return mysqlJdbcConnectionPool;
			}
			else if (BizDataSourceTypeEnum.isPgDialect(config.getDialectType())) {
				return postgreSqlJdbcConnectionPool;
			}
		}
		throw new RuntimeException("unsupported db type");
	}

	public List<DatabaseInfoBO> showDatabases(DbConfig dbConfig) throws Exception {
		return (List<DatabaseInfoBO>) accessDb(dbConfig, "showDatabases", null);
	}

	public List<SchemaInfoBO> showSchemas(DbConfig dbConfig) throws Exception {
		return (List<SchemaInfoBO>) accessDb(dbConfig, "showSchemas", null);
	}

	public List<TableInfoBO> showTables(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (List<TableInfoBO>) accessDb(dbConfig, "showTables", param);
	}

	public List<TableInfoBO> fetchTables(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (List<TableInfoBO>) accessDb(dbConfig, "fetchTables", param);
	}

	public List<ColumnInfoBO> showColumns(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (List<ColumnInfoBO>) accessDb(dbConfig, "showColumns", param);
	}

	public List<ForeignKeyInfoBO> showForeignKeys(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (List<ForeignKeyInfoBO>) accessDb(dbConfig, "showForeignKeys", param);
	}

	public List<String> sampleColumn(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (List<String>) accessDb(dbConfig, "sampleColumn", param);
	}

	public ResultSetBO scanTable(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (ResultSetBO) accessDb(dbConfig, "scanTable", param);
	}

	public ResultSetBO executeSqlAndReturnObject(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return (ResultSetBO) accessDb(dbConfig, "executeSqlAndReturnObject", param);
	}

	private Connection getConnection(DbConfig config) {
		return getConnectionPoolByType(config).getConnection(config);
	}

	private Object accessDb(DbConfig dbConfig, String method, DbQueryParameter param) throws Exception {
		if (dbConfig.getConnectionType().equals(DbAccessTypeEnum.JDBC.getCode())) {
			AbstractJdbcDdl ddlExecutor = (AbstractJdbcDdl) ddlFactory.getDdlExecutor(dbConfig);
			try (Connection connection = getConnection(dbConfig)) {
				switch (method) {
					case "showDatabases":
						return ddlExecutor.showDatabases(connection);
					case "showSchemas":
						return ddlExecutor.showSchemas(connection);
					case "showTables":
						return ddlExecutor.showTables(connection, param.getSchema(), param.getTablePattern());
					case "fetchTables":
						return ddlExecutor.fetchTables(connection, param.getSchema(), param.getTables());
					case "showColumns":
						return ddlExecutor.showColumns(connection, param.getSchema(), param.getTable());
					case "showForeignKeys":
						return ddlExecutor.showForeignKeys(connection, param.getSchema(), param.getTables());
					case "sampleColumn":
						return ddlExecutor.sampleColumn(connection, param.getSchema(), param.getTable(),
								param.getColumn());
					case "scanTable":
						return ddlExecutor.scanTable(connection, param.getSchema(), param.getTable());
					case "executeSqlAndReturnObject":
						return SqlExecutor.executeSqlAndReturnObject(connection, param.getSchema(), param.getSql());
					default:
						throw new RuntimeException("unknown method");
				}
			}
		}
		return null;
	}

}
