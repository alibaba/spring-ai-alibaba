package com.alibaba.cloud.ai.dbconnector;

import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * 负责执行 SQL 并返回结构化结果。
 */
public class SqlExecutor {

	public static final Integer RESULT_SET_LIMIT = 1000;

	public static final Integer STATEMENT_TIMEOUT = 30;

	/**
	 * 执行 SQL 查询并返回结构化结果（带列信息）
	 * @param connection 数据库连接
	 * @param sql SQL 语句
	 * @return ResultSetBO 结构化结果
	 * @throws SQLException SQL 执行异常
	 */
	public static ResultSetBO executeSqlAndReturnObject(Connection connection, String schema, String sql)
			throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.setMaxRows(RESULT_SET_LIMIT);
			statement.setQueryTimeout(STATEMENT_TIMEOUT);

			DatabaseMetaData metaData = connection.getMetaData();
			String dialect = metaData.getDatabaseProductName();

			if (dialect.equals(DatabaseDialectEnum.POSTGRESQL.code)) {
				if (StringUtils.isNotEmpty(schema)) {
					statement.execute("set search_path = '" + schema + "';");
				}
			}

			try (ResultSet rs = statement.executeQuery(sql)) {
				return ResultSetBuilder.buildFrom(rs, schema);
			}
		}
	}

	/**
	 * 执行 SQL 查询并返回字符串二维数组格式结果
	 * @param connection 数据库连接
	 * @param sql SQL 语句
	 * @return 二维数组结果
	 * @throws SQLException SQL 执行异常
	 */
	public static String[][] executeSqlAndReturnArr(Connection connection, String sql) throws SQLException {
		List<String[]> list = executeQuery(connection, sql);
		return list.toArray(new String[0][]);
	}

	public static String[][] executeSqlAndReturnArr(Connection connection, String databaseOrSchema, String sql)
			throws SQLException {
		List<String[]> list = executeQuery(connection, databaseOrSchema, sql);
		return list.toArray(new String[0][]);
	}

	private static List<String[]> executeQuery(Connection connection, String sql) throws SQLException {
		try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {

			return ResultSetConverter.convert(rs);
		}
	}

	private static List<String[]> executeQuery(Connection connection, String databaseOrSchema, String sql)
			throws SQLException {
		String originalDb = connection.getCatalog();
		DatabaseMetaData metaData = connection.getMetaData();
		String dialect = metaData.getDatabaseProductName();

		try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {

			if (dialect.equals(DatabaseDialectEnum.MYSQL.code)) {
				if (StringUtils.isNotEmpty(databaseOrSchema)) {
					statement.execute("use `" + databaseOrSchema + "`;");
				}
			}
			else if (dialect.equals(DatabaseDialectEnum.POSTGRESQL.code)) {
				if (StringUtils.isNotEmpty(databaseOrSchema)) {
					statement.execute("set search_path = '" + databaseOrSchema + "';");
				}
			}

			List<String[]> result = ResultSetConverter.convert(rs);

			if (StringUtils.isNotEmpty(databaseOrSchema) && dialect.equals(DatabaseDialectEnum.MYSQL.code)) {
				statement.execute("use `" + originalDb + "`;");
			}

			return result;
		}
	}

}