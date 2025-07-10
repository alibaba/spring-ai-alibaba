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
package com.alibaba.cloud.ai.dbconnector.bo;

import java.util.Objects;

public class TableInfoBO extends DdlBaseBO {

	private String schema;

	private String name;

	private String description;

	private String type;

	private String foreignKey;

	private String primaryKey;

	public TableInfoBO() {
	}

	public TableInfoBO(String schema, String name, String description, String type, String foreignKey,
			String primaryKey) {
		this.schema = schema;
		this.name = name;
		this.description = description;
		this.type = type;
		this.foreignKey = foreignKey;
		this.primaryKey = primaryKey;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getForeignKey() {
		return foreignKey;
	}

	public void setForeignKey(String foreignKey) {
		this.foreignKey = foreignKey;
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(String primaryKey) {
		this.primaryKey = primaryKey;
	}

	@Override
	public String toString() {
		return "TableInfoBO{" + "schema='" + schema + '\'' + ", name='" + name + '\'' + ", description='" + description
				+ '\'' + ", type='" + type + '\'' + ", foreignKey='" + foreignKey + '\'' + ", primaryKey='" + primaryKey
				+ '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TableInfoBO that = (TableInfoBO) o;
		return Objects.equals(schema, that.schema) && Objects.equals(name, that.name)
				&& Objects.equals(description, that.description) && Objects.equals(type, that.type)
				&& Objects.equals(foreignKey, that.foreignKey) && Objects.equals(primaryKey, that.primaryKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(schema, name, description, type, foreignKey, primaryKey);
	}

	public static TableInfoBOBuilder builder() {
		return new TableInfoBOBuilder();
	}

	public static final class TableInfoBOBuilder {

		private String schema;

		private String name;

		private String description;

		private String type;

		private String foreignKey;

		private String primaryKey;

		private TableInfoBOBuilder() {
		}

		public static TableInfoBOBuilder aTableInfoBO() {
			return new TableInfoBOBuilder();
		}

		public TableInfoBOBuilder schema(String schema) {
			this.schema = schema;
			return this;
		}

		public TableInfoBOBuilder name(String name) {
			this.name = name;
			return this;
		}

		public TableInfoBOBuilder description(String description) {
			this.description = description;
			return this;
		}

		public TableInfoBOBuilder type(String type) {
			this.type = type;
			return this;
		}

		public TableInfoBOBuilder foreignKey(String foreignKey) {
			this.foreignKey = foreignKey;
			return this;
		}

		public TableInfoBOBuilder primaryKey(String primaryKey) {
			this.primaryKey = primaryKey;
			return this;
		}

		public TableInfoBO build() {
			TableInfoBO tableInfoBO = new TableInfoBO();
			tableInfoBO.setSchema(schema);
			tableInfoBO.setName(name);
			tableInfoBO.setDescription(description);
			tableInfoBO.setType(type);
			tableInfoBO.setForeignKey(foreignKey);
			tableInfoBO.setPrimaryKey(primaryKey);
			return tableInfoBO;
		}

	}

}
