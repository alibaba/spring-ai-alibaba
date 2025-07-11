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

public class DatabaseInfoBO extends DdlBaseBO {

	private String name;

	private String description;

	public String getName() {
		return name;
	}

	public DatabaseInfoBO() {
	}

	public DatabaseInfoBO(String name, String description) {
		this.name = name;
		this.description = description;
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

	public static DatabaseInfoBOBuilder builder() {
		return new DatabaseInfoBOBuilder();
	}

	public static final class DatabaseInfoBOBuilder {

		private String name;

		private String description;

		private DatabaseInfoBOBuilder() {
		}

		public static DatabaseInfoBOBuilder aDatabaseInfoBO() {
			return new DatabaseInfoBOBuilder();
		}

		public DatabaseInfoBOBuilder name(String name) {
			this.name = name;
			return this;
		}

		public DatabaseInfoBOBuilder description(String description) {
			this.description = description;
			return this;
		}

		public DatabaseInfoBO build() {
			DatabaseInfoBO databaseInfoBO = new DatabaseInfoBO();
			databaseInfoBO.setName(name);
			databaseInfoBO.setDescription(description);
			return databaseInfoBO;
		}

	}

}
