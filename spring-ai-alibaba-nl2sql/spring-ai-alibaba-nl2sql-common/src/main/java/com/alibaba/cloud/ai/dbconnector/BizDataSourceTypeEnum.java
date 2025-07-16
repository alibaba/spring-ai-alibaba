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

public enum BizDataSourceTypeEnum {

	MYSQL(1, "mysql", DatabaseDialectEnum.MYSQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),

	POSTGRESQL(2, "postgresql", DatabaseDialectEnum.POSTGRESQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),

	SQLITE(3, "sqlite", DatabaseDialectEnum.MYSQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),

	HOLOGRESS(10, "hologress", DatabaseDialectEnum.POSTGRESQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),

	MYSQL_VPC(11, "mysql-vpc", DatabaseDialectEnum.MYSQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),

	POSTGRESQL_VPC(12, "postgresql-vpc", DatabaseDialectEnum.POSTGRESQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),

	ADB_PG(21, "adg_pg", DatabaseDialectEnum.POSTGRESQL.getCode(), DbAccessTypeEnum.DATA_API.getCode()),

	MAX_COMPUTE(31, "max_compute", DatabaseDialectEnum.MYSQL.getCode(), DbAccessTypeEnum.JDBC.getCode()),
	// 函数计算中SQLite模拟数据库
	FC_MEMORY_DB(41, "fc_memory_db", DatabaseDialectEnum.SQLite.getCode(), DbAccessTypeEnum.FC_HTTP.getCode()),

	MYSQL_VIRTUAL(51, "mysql-virtual", DatabaseDialectEnum.MYSQL.getCode(), DbAccessTypeEnum.MEMORY.getCode()),

	POSTGRESQL_VIRTUAL(52, "postgresql-virtual", DatabaseDialectEnum.POSTGRESQL.getCode(),
			DbAccessTypeEnum.MEMORY.getCode());

	public Integer code;

	public String typeName;

	public String dialect;

	public String protocol;

	BizDataSourceTypeEnum(Integer code, String typeName, String dialect, String protocol) {
		this.code = code;
		this.typeName = typeName;
		this.dialect = dialect;
		this.protocol = protocol;
	}

	public Integer getCode() {
		return code;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getDialect() {
		return dialect;
	}

	/**
	 * 根据code获取对应的typeName。
	 * @param code 要获取typeName的code
	 * @return 对应的typeName，如果没有找到则返回null。
	 */
	public static String getTypeNameByCode(Integer code) {
		for (BizDataSourceTypeEnum type : values()) {
			if (type.getCode().equals(code)) {
				return type.getTypeName();
			}
		}
		return null; // 如果没有找到对应的code，则返回null
	}

	public static String getDialectByCode(Integer code) {
		for (BizDataSourceTypeEnum type : values()) {
			if (type.getCode().equals(code)) {
				return type.getDialect();
			}
		}
		return null; // 如果没有找到对应的code，则返回null
	}

	public static String getProtocolByCode(Integer code) {
		for (BizDataSourceTypeEnum type : values()) {
			if (type.getCode().equals(code)) {
				return type.getProtocol();
			}
		}
		return null;
	}

	public static BizDataSourceTypeEnum fromCode(Integer code) {
		for (BizDataSourceTypeEnum type : values()) {
			if (type.getCode() == code) {
				return type;
			}
		}
		return null;
	}

	public static BizDataSourceTypeEnum fromTypeName(String typeName) {
		for (BizDataSourceTypeEnum type : values()) {
			if (type.getTypeName().equals(typeName)) {
				return type;
			}
		}
		return null;
	}

	public static boolean isMysqlDialect(String typeName) {
		return isDialect(typeName, DatabaseDialectEnum.MYSQL.getCode());
	}

	public static boolean isPgDialect(String typeName) {
		return isDialect(typeName, DatabaseDialectEnum.POSTGRESQL.getCode());
	}

	public static boolean isAdbPg(String typeName) {
		BizDataSourceTypeEnum te = fromTypeName(typeName);
		if (te == null) {
			return false;
		}
		if (DatabaseDialectEnum.POSTGRESQL.getCode().equalsIgnoreCase(te.getDialect())
				&& DbAccessTypeEnum.DATA_API.getCode().equalsIgnoreCase(te.getProtocol())) {
			return true;
		}
		return false;
	}

	public static boolean isDialect(String typeName, String dialect) {
		BizDataSourceTypeEnum te = fromTypeName(typeName);
		if (te == null) {
			return false;
		}
		if (dialect.equalsIgnoreCase(te.getDialect())) {
			return true;
		}
		return false;
	}

}
