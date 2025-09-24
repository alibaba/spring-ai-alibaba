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

package com.alibaba.cloud.ai.connector.accessor.defaults;

import com.alibaba.cloud.ai.connector.AbstractJdbcDdl;
import com.alibaba.cloud.ai.connector.DBConnectionPool;
import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.support.DdlFactory;
import com.alibaba.cloud.ai.connector.SqlExecutor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DatabaseInfoBO;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.ResultSetBO;
import com.alibaba.cloud.ai.connector.bo.SchemaInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.List;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public abstract class AbstractAccessor implements Accessor {

	private static final Logger logger = LoggerFactory.getLogger(AbstractAccessor.class);

	private final DdlFactory ddlFactory;

	private final DBConnectionPool dbConnectionPool;

	protected AbstractAccessor(DdlFactory ddlFactory, DBConnectionPool dbConnectionPool) {
		this.dbConnectionPool = dbConnectionPool;
		this.ddlFactory = ddlFactory;
	}

	protected abstract String getDbAccessorType();

	public <T> T accessDb(DbConfig dbConfig, String method, DbQueryParameter param) throws Exception {

		try (Connection connection = getConnection(dbConfig)) {

			AbstractJdbcDdl ddlExecutor = (AbstractJdbcDdl) ddlFactory.getDdlExecutor(dbConfig);

			switch (method) {
				case "showDatabases":
					return (T) ddlExecutor.showDatabases(connection);
				case "showSchemas":
					return (T) ddlExecutor.showSchemas(connection);
				case "showTables":
					return (T) ddlExecutor.showTables(connection, param.getSchema(), param.getTablePattern());
				case "fetchTables":
					return (T) ddlExecutor.fetchTables(connection, param.getSchema(), param.getTables());
				case "showColumns":
					return (T) ddlExecutor.showColumns(connection, param.getSchema(), param.getTable());
				case "showForeignKeys":
					return (T) ddlExecutor.showForeignKeys(connection, param.getSchema(), param.getTables());
				case "sampleColumn":
					return (T) ddlExecutor.sampleColumn(connection, param.getSchema(), param.getTable(),
							param.getColumn());
				case "scanTable":
					return (T) ddlExecutor.scanTable(connection, param.getSchema(), param.getTable());
				case "executeSqlAndReturnObject":
					return (T) SqlExecutor.executeSqlAndReturnObject(connection, param.getSchema(), param.getSql());
				default:
					throw new UnsupportedOperationException("Unknown method: " + method);
			}
		}
		catch (Exception e) {

			logger.error("Error accessing database with method: {}", method, e);
			throw e;
		}
	}

	public List<DatabaseInfoBO> showDatabases(DbConfig dbConfig) throws Exception {
		return accessDb(dbConfig, "showDatabases", null);
	}

	public List<SchemaInfoBO> showSchemas(DbConfig dbConfig) throws Exception {
		return accessDb(dbConfig, "showSchemas", null);
	}

	public List<TableInfoBO> showTables(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "showTables", param);
	}

	public List<TableInfoBO> fetchTables(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "fetchTables", param);
	}

	public List<ColumnInfoBO> showColumns(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "showColumns", param);
	}

	public List<ForeignKeyInfoBO> showForeignKeys(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "showForeignKeys", param);
	}

	public List<String> sampleColumn(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "sampleColumn", param);
	}

	public ResultSetBO scanTable(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "scanTable", param);
	}

	public ResultSetBO executeSqlAndReturnObject(DbConfig dbConfig, DbQueryParameter param) throws Exception {
		return accessDb(dbConfig, "executeSqlAndReturnObject", param);
	}

	public Connection getConnection(DbConfig config) {
		return this.dbConnectionPool.getConnection(config);
	}

}
