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
package com.alibaba.cloud.ai.request;

import com.alibaba.cloud.ai.dbconnector.DbConfig;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class SchemaInitRequest implements Serializable {

	private DbConfig dbConfig;

	private List<String> tables;

	public DbConfig getDbConfig() {
		return dbConfig;
	}

	public void setDbConfig(DbConfig dbConfig) {
		this.dbConfig = dbConfig;
	}

	public List<String> getTables() {
		return tables;
	}

	public void setTables(List<String> tables) {
		this.tables = tables;
	}

	@Override
	public String toString() {
		return "SchemaInitRequest{" + "dbConfig=" + dbConfig + ", tables=" + tables + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		SchemaInitRequest that = (SchemaInitRequest) o;
		return Objects.equals(dbConfig, that.dbConfig) && Objects.equals(tables, that.tables);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dbConfig, tables);
	}

}
