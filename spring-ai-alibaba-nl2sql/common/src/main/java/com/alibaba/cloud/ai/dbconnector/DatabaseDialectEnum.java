package com.alibaba.cloud.ai.dbconnector;

public enum DatabaseDialectEnum {

	MYSQL("MySQL"),

	SQLite("SQLite"),

	POSTGRESQL("PostgreSQL");

	public String code;

	DatabaseDialectEnum(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

}
