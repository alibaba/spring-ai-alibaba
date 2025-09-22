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
package com.alibaba.cloud.ai.connector.h2;

import com.alibaba.cloud.ai.connector.AbstractJdbcDdl;
import com.alibaba.cloud.ai.connector.SqlExecutor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DatabaseInfoBO;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.ResultSetBO;
import com.alibaba.cloud.ai.connector.bo.SchemaInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.enums.BizDataSourceTypeEnum;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.connector.ColumnTypeParser.wrapType;

@Service
public class H2JdbcDdl extends AbstractJdbcDdl {

	@Override
	public List<DatabaseInfoBO> showDatabases(Connection connection) {
		String sql = "SELECT CATALOG_NAME FROM INFORMATION_SCHEMA.INFORMATION_SCHEMA_CATALOG_NAME;";
		List<DatabaseInfoBO> databaseInfoList = Lists.newArrayList();
		try {
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, sql);
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0) {
					continue;
				}
				String database = resultArr[i][0];
				databaseInfoList.add(DatabaseInfoBO.builder().name(database).build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return databaseInfoList;
	}

	@Override
	public List<SchemaInfoBO> showSchemas(Connection connection) {
		String sql = "SELECT schema_name FROM information_schema.schemata;";
		List<SchemaInfoBO> schemaInfoList = Lists.newArrayList();
		try {
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, sql);
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0) {
					continue;
				}
				String database = resultArr[i][0];
				schemaInfoList.add(SchemaInfoBO.builder().name(database).build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return schemaInfoList;
	}

	@Override
	public List<TableInfoBO> showTables(Connection connection, String schema, String tablePattern) {
		String sql = "SELECT TABLE_NAME, REMARKS \n" + "FROM INFORMATION_SCHEMA.TABLES \n"
				+ "WHERE TABLE_SCHEMA = '%s' \n";
		if (StringUtils.isNotBlank(tablePattern)) {
			sql += "AND TABLE_NAME LIKE CONCAT('%%','%s','%%') \n";
		}
		sql += "limit 2000;";
		List<TableInfoBO> tableInfoList = Lists.newArrayList();
		try {
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection,
					String.format(sql, schema, tablePattern));
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0) {
					continue;
				}
				String tableName = resultArr[i][0];
				String tableDesc = resultArr[i][1];
				tableInfoList.add(TableInfoBO.builder().schema(schema).name(tableName).description(tableDesc).build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return tableInfoList;
	}

	@Override
	public List<TableInfoBO> fetchTables(Connection connection, String schema, List<String> tables) {
		String sql = "SELECT TABLE_NAME, REMARKS \n" + "FROM INFORMATION_SCHEMA.TABLES \n"
				+ "WHERE TABLE_SCHEMA = '%s' \n" + "AND TABLE_NAME in(%s) \n" + "limit 200;";
		List<TableInfoBO> tableInfoList = Lists.newArrayList();
		String tableListStr = String.join(", ", tables.stream().map(x -> "'" + x + "'").collect(Collectors.toList()));
		try {
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection,
					String.format(sql, schema, tableListStr));
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0) {
					continue;
				}
				String tableName = resultArr[i][0];
				String tableDesc = resultArr[i][1];
				tableInfoList.add(TableInfoBO.builder().schema(schema).name(tableName).description(tableDesc).build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return tableInfoList;
	}

	@Override
	public List<ColumnInfoBO> showColumns(Connection connection, String schema, String table) {
		String sql = "SELECT column_name, remarks, data_type, \n"
				+ "CASE WHEN IS_IDENTITY = 'YES' THEN TRUE ELSE FALSE END AS 主键唯一, \n"
				+ "CASE WHEN IS_NULLABLE = 'NO' THEN TRUE ELSE FALSE END AS 非空 \n" + "FROM information_schema.COLUMNS "
				+ "WHERE table_schema='%s' " + "and table_name='%s';";
		List<ColumnInfoBO> columnInfoList = Lists.newArrayList();
		try {
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, "INFORMATION_SCHEMA",
					String.format(sql, schema, table));
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0) {
					continue;
				}
				columnInfoList.add(ColumnInfoBO.builder()
					.name(resultArr[i][0])
					.description(resultArr[i][1])
					.type(wrapType(resultArr[i][2]))
					.primary(BooleanUtils.toBoolean(resultArr[i][3]))
					.notnull(BooleanUtils.toBoolean(resultArr[i][4]))
					.build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return columnInfoList;
	}

	@Override
	public List<ForeignKeyInfoBO> showForeignKeys(Connection connection, String schema, List<String> tables) {
		String sql = "SELECT \n" + "    kc.TABLE_NAME AS 表名,\n" + "    kc.COLUMN_NAME AS 列名,\n"
				+ "    kc2.table_name AS 引用表名,\n" + "    kc2.column_name AS 引用列名\n" + "FROM \n"
				+ "    information_schema.referential_constraints rc  \n"
				+ "    join information_schema.key_column_usage kc on rc.constraint_name=kc.constraint_name \n"
				+ "    join information_schema.key_column_usage kc2 on rc.unique_constraint_name=kc2.constraint_name \n"
				+ "WHERE kc.constraint_schema='%s' and kc.table_name in (%s) and kc2.table_name in (%s);";
		List<ForeignKeyInfoBO> foreignKeyInfoList = Lists.newArrayList();
		String tableListStr = String.join(", ", tables.stream().map(x -> "'" + x + "'").collect(Collectors.toList()));

		try {
			sql = String.format(sql, schema, tableListStr, tableListStr);
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, "INFORMATION_SCHEMA", sql);
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0) {
					continue;
				}
				foreignKeyInfoList.add(ForeignKeyInfoBO.builder()
					.table(resultArr[i][0])
					.column(resultArr[i][1])
					.referencedTable(resultArr[i][2])
					.referencedColumn(resultArr[i][3])
					.build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return foreignKeyInfoList;
	}

	@Override
	public List<String> sampleColumn(Connection connection, String schema, String table, String column) {
		String sql = "SELECT \n" + "    `%s`\n" + "FROM \n" + "    `%s`.`%s`\n" + "LIMIT 99;";
		List<String> sampleInfo = Lists.newArrayList();
		try {
			sql = String.format(sql, column, schema, table);
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, null, sql);
			if (resultArr.length <= 1) {
				return Lists.newArrayList();
			}

			for (int i = 1; i < resultArr.length; i++) {
				if (resultArr[i].length == 0 || column.equalsIgnoreCase(resultArr[i][0])) {
					continue;
				}
				sampleInfo.add(resultArr[i][0]);
			}
		}
		catch (SQLException e) {
			// throw new RuntimeException(e);
		}

		Set<String> siSet = sampleInfo.stream().collect(Collectors.toSet());
		sampleInfo = siSet.stream().collect(Collectors.toList());
		return sampleInfo;
	}

	@Override
	public ResultSetBO scanTable(Connection connection, String schema, String table) {
		String sql = "SELECT *\n" + "FROM \n" + "    `%s`\n" + "LIMIT 20;";
		ResultSetBO resultSet = ResultSetBO.builder().build();
		try {
			resultSet = SqlExecutor.executeSqlAndReturnObject(connection, schema, String.format(sql, table));
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return resultSet;
	}

	@Override
	public BizDataSourceTypeEnum getType() {
		return BizDataSourceTypeEnum.H2;
	}

}
