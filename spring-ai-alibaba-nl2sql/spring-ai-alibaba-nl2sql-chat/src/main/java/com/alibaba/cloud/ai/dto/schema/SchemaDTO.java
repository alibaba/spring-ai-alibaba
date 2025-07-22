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
package com.alibaba.cloud.ai.dto.schema;

import java.util.List;

public class SchemaDTO {

	private String name;

	private String description;

	private Integer tableCount;

	private List<TableDTO> table;

	private List<List<String>> foreignKeys;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getTableCount() {
		return tableCount;
	}

	public void setTableCount(Integer tableCount) {
		this.tableCount = tableCount;
	}

	public List<TableDTO> getTable() {
		return table;
	}

	public void setTable(List<TableDTO> table) {
		this.table = table;
	}

	public List<List<String>> getForeignKeys() {
		return foreignKeys;
	}

	public void setForeignKeys(List<List<String>> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}

	@Override
	public String toString() {
		return "SchemaDTO{" + "name='" + name + '\'' + ", description='" + description + '\'' + ", tableCount="
				+ tableCount + ", table=" + table + ", foreignKeys=" + foreignKeys + '}';
	}

}
