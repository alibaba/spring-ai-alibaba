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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.dbconnector.ColumnTypeParser.wrapType;

/**
 * @author jiuhe
 * @since 2024/3/15
 */
@Slf4j
@Service
@AllArgsConstructor
public class PostgreJdbcDdl extends AbstractJdbcDdl {

	@Override
	public List<DatabaseInfoBO> showDatabases(Connection connection) {
		String sql = "select datname from pg_database;";
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
		String sql = "SELECT schema_name \n" + "FROM information_schema.schemata;";
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
		String sql = "SELECT tb.table_name, d.description \n" + "FROM information_schema.tables tb \n"
				+ "JOIN pg_class c ON c.relname = tb.table_name \n"
				+ "JOIN pg_namespace n ON c.relnamespace = n.oid and tb.table_schema = n.nspname \n"
				+ "LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = '0' \n"
				+ "WHERE tb.table_schema = '%s' \n";
		if (StringUtils.isNotBlank(tablePattern)) {
			sql += "AND tb.table_name LIKE CONCAT('%%','%s','%%') \n";
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
				tableInfoList.add(TableInfoBO.builder().name(tableName).description(tableDesc).build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return tableInfoList;
	}

	@Override
	public List<TableInfoBO> fetchTables(Connection connection, String schema, List<String> tables) {
		String sql = "SELECT tb.table_name, d.description \n" + "FROM information_schema.tables tb \n"
				+ "JOIN pg_class c ON c.relname = tb.table_name \n"
				+ "JOIN pg_namespace n ON c.relnamespace = n.oid and tb.table_schema = n.nspname \n"
				+ "LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = '0' \n"
				+ "WHERE tb.table_schema = '%s' \n" + "AND tb.table_name IN (%s) \n" + " limit 2000;";

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
				tableInfoList.add(TableInfoBO.builder().name(tableName).description(tableDesc).build());
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return tableInfoList;
	}

	@Override
	public List<ColumnInfoBO> showColumns(Connection connection, String schema, String table) {
		String sql = "\n" + "SELECT\n" + "    a.attname as column_name,\n"
				+ "    col_description(a.attrelid, a.attnum) as column_description,\n"
				+ "    pg_catalog.format_type(a.atttypid, a.atttypmod) as data_type,\n" + "    CASE\n"
				+ "        WHEN a.attnum = ANY (ind.indkey) THEN true\n" + "        ELSE false\n" + "    END as 主键唯一,\n"
				+ "     a.attnotnull as 非空\n" + "FROM\n" + "    pg_catalog.pg_attribute a\n" + "LEFT JOIN\n"
				+ "    pg_catalog.pg_index ind ON ind.indrelid = a.attrelid AND ind.indisprimary\n" + "LEFT JOIN\n"
				+ "    pg_catalog.pg_class c ON a.attrelid = c.oid\n" + "LEFT JOIN\n"
				+ "    pg_catalog.pg_namespace n ON n.oid = c.relnamespace\n" + "WHERE\n" + "    c.relname = '%s'\n"
				+ "    AND a.attnum > 0\n" + "    AND NOT a.attisdropped\n" + "    AND n.nspname = '%s'\n"
				+ "ORDER BY\n" + "    a.attnum;";
		List<ColumnInfoBO> columnInfoList = Lists.newArrayList();
		try {
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, null,
					String.format(sql, table, schema));
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
		String sql = "SELECT\n" + "    tc.table_name,\n" + "    kcu.column_name,\n" + "    tc.constraint_name,\n"
				+ "    ccu.table_name AS foreign_table_name,\n" + "    ccu.column_name AS foreign_column_name\n"
				+ "FROM\n" + "    information_schema.table_constraints AS tc\n" + "JOIN\n"
				+ "    information_schema.key_column_usage AS kcu\n"
				+ "    ON tc.constraint_name = kcu.constraint_name\n" + "    AND tc.table_schema = kcu.table_schema\n"
				+ "JOIN\n" + "    information_schema.constraint_column_usage AS ccu\n"
				+ "    ON ccu.constraint_name = tc.constraint_name\n" + "    AND ccu.table_schema = tc.table_schema\n"
				+ "WHERE\n" + "    tc.constraint_type = 'FOREIGN KEY'\n" + "    AND tc.table_schema='public'\n"
				+ "    AND tc.table_name in (%s)";
		List<ForeignKeyInfoBO> foreignKeyInfoList = Lists.newArrayList();
		String tableListStr = String.join(", ", tables.stream().map(x -> "'" + x + "'").collect(Collectors.toList()));

		try {
			sql = String.format(sql, tableListStr);
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, null, sql);
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
					.referencedTable(resultArr[i][3])
					.referencedColumn(resultArr[i][4])
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
		String sql = "SELECT \n" + "    \"%s\"\n" + "FROM \n" + "    \"%s\"\n" + "LIMIT 99;";
		List<String> sampleInfo = Lists.newArrayList();
		try {
			sql = String.format(sql, column, table);
			String[][] resultArr = SqlExecutor.executeSqlAndReturnArr(connection, schema, sql);
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
			log.error("sampleColumn error, sql:{}", sql);
			log.error("sampleColumn error", e);
		}

		Set<String> siSet = sampleInfo.stream().collect(Collectors.toSet());
		sampleInfo = siSet.stream().collect(Collectors.toList());
		return sampleInfo;
	}

	@Override
	public ResultSetBO scanTable(Connection connection, String schema, String table) {
		String sql = "SELECT *\n" + "FROM \n" + "    %s\n" + "LIMIT 20;";
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
		return BizDataSourceTypeEnum.POSTGRESQL;
	}

}
