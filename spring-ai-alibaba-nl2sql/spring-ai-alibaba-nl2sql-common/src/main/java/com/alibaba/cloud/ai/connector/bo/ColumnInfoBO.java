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

public class ColumnInfoBO extends DdlBaseBO {

	public ColumnInfoBO() {
	}

	public ColumnInfoBO(String name, String tableName, String description, String type, boolean primary,
			boolean notnull, String samples) {
		this.name = name;
		this.tableName = tableName;
		this.description = description;
		this.type = type;
		this.primary = primary;
		this.notnull = notnull;
		this.samples = samples;
	}

	private String name;

	private String tableName;

	private String description;

	private String type;

	private boolean primary;

	private boolean notnull;

	private String samples;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
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

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public boolean isNotnull() {
		return notnull;
	}

	public void setNotnull(boolean notnull) {
		this.notnull = notnull;
	}

	public String getSamples() {
		return samples;
	}

	public void setSamples(String samples) {
		this.samples = samples;
	}

	@Override
	public String toString() {
		return "ColumnInfoBO{" + "name='" + name + '\'' + ", tableName='" + tableName + '\'' + ", description='"
				+ description + '\'' + ", type='" + type + '\'' + ", primary=" + primary + ", notnull=" + notnull
				+ ", samples='" + samples + '\'' + '}';
	}

	public static ColumnInfoBOBuilder builder() {
		return new ColumnInfoBOBuilder();
	}

	public static final class ColumnInfoBOBuilder {

		private String name;

		private String tableName;

		private String description;

		private String type;

		private boolean primary;

		private boolean notnull;

		private String samples;

		private ColumnInfoBOBuilder() {
		}

		public static ColumnInfoBOBuilder aColumnInfoBO() {
			return new ColumnInfoBOBuilder();
		}

		public ColumnInfoBOBuilder name(String name) {
			this.name = name;
			return this;
		}

		public ColumnInfoBOBuilder tableName(String tableName) {
			this.tableName = tableName;
			return this;
		}

		public ColumnInfoBOBuilder description(String description) {
			this.description = description;
			return this;
		}

		public ColumnInfoBOBuilder type(String type) {
			this.type = type;
			return this;
		}

		public ColumnInfoBOBuilder primary(boolean primary) {
			this.primary = primary;
			return this;
		}

		public ColumnInfoBOBuilder notnull(boolean notnull) {
			this.notnull = notnull;
			return this;
		}

		public ColumnInfoBOBuilder samples(String samples) {
			this.samples = samples;
			return this;
		}

		public ColumnInfoBO build() {
			ColumnInfoBO columnInfoBO = new ColumnInfoBO();
			columnInfoBO.setName(name);
			columnInfoBO.setTableName(tableName);
			columnInfoBO.setDescription(description);
			columnInfoBO.setType(type);
			columnInfoBO.setPrimary(primary);
			columnInfoBO.setNotnull(notnull);
			columnInfoBO.setSamples(samples);
			return columnInfoBO;
		}

	}

}
