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

package com.alibaba.cloud.ai.connector.accessor;

import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DatabaseInfoBO;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.ResultSetBO;
import com.alibaba.cloud.ai.connector.bo.SchemaInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;

import java.util.List;

/**
 * Data access interface definition.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public interface Accessor {

	/**
	 * Access the database and execute the specified method with the given parameters.
	 * @param dbConfig database configuration
	 * @param method method name
	 * @param param query parameters
	 * @return result object, which can be a list of database information, schema
	 * information, table information, etc.
	 * @throws Exception if an error occurs during database access
	 */
	<T> T accessDb(DbConfig dbConfig, String method, DbQueryParameter param) throws Exception;

	List<DatabaseInfoBO> showDatabases(DbConfig dbConfig) throws Exception;

	List<SchemaInfoBO> showSchemas(DbConfig dbConfig) throws Exception;

	List<TableInfoBO> showTables(DbConfig dbConfig, DbQueryParameter param) throws Exception;

	List<TableInfoBO> fetchTables(DbConfig dbConfig, DbQueryParameter param) throws Exception;

	List<ColumnInfoBO> showColumns(DbConfig dbConfig, DbQueryParameter param) throws Exception;

	List<ForeignKeyInfoBO> showForeignKeys(DbConfig dbConfig, DbQueryParameter param) throws Exception;

	List<String> sampleColumn(DbConfig dbConfig, DbQueryParameter param) throws Exception;

	ResultSetBO scanTable(DbConfig dbConfig, DbQueryParameter param) throws Exception;

	ResultSetBO executeSqlAndReturnObject(DbConfig dbConfig, DbQueryParameter param) throws Exception;

}
