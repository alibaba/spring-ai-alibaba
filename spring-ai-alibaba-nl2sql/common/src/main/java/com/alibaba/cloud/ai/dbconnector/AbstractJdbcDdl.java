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

import com.alibaba.cloud.ai.dbconnector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.DatabaseInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.dbconnector.bo.SchemaInfoBO;
import com.alibaba.cloud.ai.dbconnector.bo.TableInfoBO;
import org.springframework.beans.factory.InitializingBean;

import java.sql.Connection;
import java.util.List;

public abstract class AbstractJdbcDdl extends AbstractDdl implements InitializingBean {

	@Override
	public void afterPropertiesSet() throws Exception {
		DdlFactory.registry(this);
	}

	@Deprecated
	public abstract List<DatabaseInfoBO> showDatabases(Connection connection);

	public abstract List<SchemaInfoBO> showSchemas(Connection connection);

	public abstract List<TableInfoBO> showTables(Connection connection, String schema, String tablePattern);

	public abstract List<TableInfoBO> fetchTables(Connection connection, String schema, List<String> tables);

	public abstract List<ColumnInfoBO> showColumns(Connection connection, String schema, String table);

	public abstract List<ForeignKeyInfoBO> showForeignKeys(Connection connection, String schema, List<String> tables);

	public abstract List<String> sampleColumn(Connection connection, String schema, String table, String column);

	public abstract ResultSetBO scanTable(Connection connection, String schema, String table);

}
