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
package com.alibaba.cloud.ai.connector.bo;

import com.alibaba.cloud.ai.connector.config.DbConfig;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Objects;

public class DbQueryParameter {

	private String aliuid;

	private String workspaceId;

	private String region;

	private String secretArn;

	private String dbInstanceId;

	private String database;

	private String schema;

	private String table;

	private String tablePattern;

	private List<String> tables;

	private String column;

	private String sql;

	public DbQueryParameter() {
	}

	public DbQueryParameter(String aliuid, String workspaceId, String region, String secretArn, String dbInstanceId,
			String database, String schema, String table, String tablePattern, List<String> tables, String column,
			String sql) {
		this.aliuid = aliuid;
		this.workspaceId = workspaceId;
		this.region = region;
		this.secretArn = secretArn;
		this.dbInstanceId = dbInstanceId;
		this.database = database;
		this.schema = schema;
		this.table = table;
		this.tablePattern = tablePattern;
		this.tables = tables;
		this.column = column;
		this.sql = sql;
	}

	public String getAliuid() {
		return aliuid;
	}

	public DbQueryParameter setAliuid(String aliuid) {
		this.aliuid = aliuid;
		return this;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public DbQueryParameter setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
		return this;
	}

	public String getRegion() {
		return region;
	}

	public DbQueryParameter setRegion(String region) {
		this.region = region;
		return this;
	}

	public String getSecretArn() {
		return secretArn;
	}

	public DbQueryParameter setSecretArn(String secretArn) {
		this.secretArn = secretArn;
		return this;
	}

	public String getDbInstanceId() {
		return dbInstanceId;
	}

	public DbQueryParameter setDbInstanceId(String dbInstanceId) {
		this.dbInstanceId = dbInstanceId;
		return this;
	}

	public String getDatabase() {
		return database;
	}

	public DbQueryParameter setDatabase(String database) {
		this.database = database;
		return this;
	}

	public String getSchema() {
		return schema;
	}

	public DbQueryParameter setSchema(String schema) {
		this.schema = schema;
		return this;
	}

	public String getTable() {
		return table;
	}

	public DbQueryParameter setTable(String table) {
		this.table = table;
		return this;
	}

	public String getTablePattern() {
		return tablePattern;
	}

	public DbQueryParameter setTablePattern(String tablePattern) {
		this.tablePattern = tablePattern;
		return this;
	}

	public List<String> getTables() {
		return tables;
	}

	public DbQueryParameter setTables(List<String> tables) {
		this.tables = tables;
		return this;
	}

	public String getColumn() {
		return column;
	}

	public DbQueryParameter setColumn(String column) {
		this.column = column;
		return this;
	}

	public String getSql() {
		return sql;
	}

	public DbQueryParameter setSql(String sql) {
		this.sql = sql;
		return this;
	}

	public static DbQueryParameter from(DbConfig config) {
		DbQueryParameter param = new DbQueryParameter();
		BeanUtils.copyProperties(config, param);
		return param;
	}

	@Override
	public String toString() {
		return "DbQueryParameter{" + "aliuid='" + aliuid + '\'' + ", workspaceId='" + workspaceId + '\'' + ", region='"
				+ region + '\'' + ", secretArn='" + secretArn + '\'' + ", dbInstanceId='" + dbInstanceId + '\''
				+ ", database='" + database + '\'' + ", schema='" + schema + '\'' + ", table='" + table + '\''
				+ ", tablePattern='" + tablePattern + '\'' + ", tables=" + tables + ", column='" + column + '\''
				+ ", sql='" + sql + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DbQueryParameter that = (DbQueryParameter) o;
		return Objects.equals(aliuid, that.aliuid) && Objects.equals(workspaceId, that.workspaceId)
				&& Objects.equals(region, that.region) && Objects.equals(secretArn, that.secretArn)
				&& Objects.equals(dbInstanceId, that.dbInstanceId) && Objects.equals(database, that.database)
				&& Objects.equals(schema, that.schema) && Objects.equals(table, that.table)
				&& Objects.equals(tablePattern, that.tablePattern) && Objects.equals(tables, that.tables)
				&& Objects.equals(column, that.column) && Objects.equals(sql, that.sql);
	}

	@Override
	public int hashCode() {
		return Objects.hash(aliuid, workspaceId, region, secretArn, dbInstanceId, database, schema, table, tablePattern,
				tables, column, sql);
	}

}
