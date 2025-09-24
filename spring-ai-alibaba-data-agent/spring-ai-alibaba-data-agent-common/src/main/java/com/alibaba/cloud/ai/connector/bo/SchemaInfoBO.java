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

public class SchemaInfoBO extends DdlBaseBO {

	private String name;

	private String description;

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

	@Override
	public String toString() {
		return "SchemaInfoBO{" + "name='" + name + '\'' + ", description='" + description + '\'' + '}';
	}

	public static SchemaInfoBOBuilder builder() {
		return new SchemaInfoBOBuilder();
	}

	public static final class SchemaInfoBOBuilder {

		private String name;

		private String description;

		public SchemaInfoBOBuilder() {
		}

		public SchemaInfoBOBuilder(SchemaInfoBO other) {
			this.name = other.name;
			this.description = other.description;
		}

		public static SchemaInfoBOBuilder aSchemaInfoBO() {
			return new SchemaInfoBOBuilder();
		}

		public SchemaInfoBOBuilder name(String name) {
			this.name = name;
			return this;
		}

		public SchemaInfoBOBuilder description(String description) {
			this.description = description;
			return this;
		}

		public SchemaInfoBO build() {
			SchemaInfoBO schemaInfoBO = new SchemaInfoBO();
			schemaInfoBO.setName(name);
			schemaInfoBO.setDescription(description);
			return schemaInfoBO;
		}

	}

}
