package com.alibaba.cloud.ai.dbconnector;

import com.alibaba.cloud.ai.dbconnector.bo.*;
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
