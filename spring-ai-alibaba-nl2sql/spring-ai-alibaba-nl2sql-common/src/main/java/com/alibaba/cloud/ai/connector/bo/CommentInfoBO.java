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

public class CommentInfoBO extends DdlBaseBO {

	public CommentInfoBO() {
	}

	public CommentInfoBO(String schema, String table, String column, String description) {
		this.schema = schema;
		this.table = table;
		this.column = column;
		this.description = description;
	}

	private String schema;

	private String table;

	private String column;

	private String description;

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "CommentInfoBO{" + "schema='" + schema + '\'' + ", table='" + table + '\'' + ", column='" + column + '\''
				+ ", description='" + description + '\'' + '}';
	}

}
