package com.alibaba.cloud.ai.dbconnector;

import org.springframework.stereotype.Service;

import static com.alibaba.cloud.ai.dbconnector.ErrorCodeEnum.*;

@Service
public class MysqlJdbcConnectionPool extends AbstractDBConnectionPool {

	@Override
	public DatabaseDialectEnum getDialect() {
		return DatabaseDialectEnum.MYSQL;
	}

	@Override
	public String getDriver() {
		return "com.mysql.cj.jdbc.Driver";
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